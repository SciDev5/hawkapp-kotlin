package wsTransaction

import util.bigEndianBytes
import util.bigEndianLong
import util.coroutine.KPromise
import util.coroutine.ToggleLock
import util.coroutine.resolve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class KWSTransactor(
    private val ws: CommonWebsocket,
    private val scope: CoroutineScope,
) {
    private val transactions = mutableMapOf<Long, Transaction>()
    private val beginningTransactions = mutableMapOf<Long, KPromise<Unit>>()
    private val handlers = mutableMapOf<String, suspend Transaction.Scope.() -> Unit>()

    init {
        ws.addReceiveBinaryHandle(this::receiveBinaryRaw)
        scope.launch {
            ws.waitClosed()
            for (tx in transactions.values.toList())
                tx.end()
        }
    }

    private suspend fun receiveBinaryRaw(byteArray: ByteArray) {
        if (byteArray.size < 9)
            throw Error("KWSTx receive :: received length too short")
        receiveBinary(
            Op.opById[byteArray[0]] ?: throw Error("KWSTx receive :: operation ${byteArray[0]} not found"),
            byteArray.bigEndianLong(1),
            if (byteArray.size == 9)
                null
            else
                byteArray.sliceArray(9 until byteArray.size).decodeToString(),
        )
    }

    private var receivedHello = false
    private val receivedHelloPromise = KPromise<Unit>()
    suspend fun sendHello() {
        ws.waitOpenOrClose()
        if (!receivedHello)
            send(Op.HELLO, 0, null)
    }

    suspend fun waitForHello() = receivedHelloPromise.await()
    suspend fun waitForClose() = ws.waitClosed()

    private fun logIO(sr: String, op: Op, txId: Long, data: String?) =
        println("KWS $sr: $op, ${txId.toULong().toString(16)}${data?.let { ", '$it'" } ?: ""}")

    private suspend fun receiveBinary(op: Op, txId: Long, data: String?) {
        logIO("RECEIVE\t", op, txId, data)
        when (op) {
            Op.HELLO -> {
                receivedHello = true
                receivedHelloPromise.resolve()
                send(Op.HELLO_ACK, 0, null)
            }

            Op.HELLO_ACK -> {
                receivedHello = true
                receivedHelloPromise.resolve()
            }

            Op.BEGIN -> receiveBegin(txId, data)
            Op.BEGIN_ACK -> receiveBeginAcknowledge(txId)

            Op.END -> receiveEnd(txId)

            Op.DATA -> receiveData(txId, data)
        }
    }


    private suspend fun send(op: Op, txId: Long, data: String?) {
        logIO("SEND\t", op, txId, data)
        ws.sendBinary(
            byteArrayOf(
                op.id,
                *txId.bigEndianBytes(),
                *data?.encodeToByteArray() ?: byteArrayOf(),
            )
        )
    }

    private suspend inline fun <reified T> send(op: Op, txId: Long, data: T) =
        send(op, txId, Json.encodeToString(data))

    enum class Op(val id: Byte) {
        HELLO(0x00),
        HELLO_ACK(0x01),
        BEGIN(0x20),
        BEGIN_ACK(0x21),
        END(0x22),
        DATA(0x40);

        companion object {
            val opById = mapOf(*values().map { it.id to it }.toTypedArray())
        }
    }

    val transactionEndedObject = Error("Transaction Ended")

    fun handle(key: String, handler: suspend Transaction.Scope.() -> Unit) {
        if (key in handlers) throw Error("multiple transactions assigned to handler '$key'")
        handlers[key] = handler
    }

    fun handle(handler: KWSTransactionHandle) =
        handle(handler.key, handler.block)

    suspend fun <R> run(key: String, handler: suspend Transaction.Scope.() -> R): R {
        if (!ws.open)
            throw Error("Cannot run transaction on non-open websocket")

        val txId = Random.nextLong()
        val acknowledged = KPromise<Unit>()
        beginningTransactions[txId] = acknowledged
        send(Op.BEGIN, txId, MessageBegin(key))

        val transaction = Transaction(txId)

        acknowledged.await()

        return transaction.runTx(handler)
    }

    @Serializable
    private class MessageBegin(val key: String)

    private fun receiveBegin(txId: Long, data: String?) {
        val dataParsed = Json.decodeFromString<MessageBegin>(
            data ?: throw Error("begin was not passed any data")
        )
        val key = dataParsed.key

        val handler = handlers[key] ?: throw Error("handler not found for '$key'")

        scope.launch {
            val transaction = Transaction(txId)
            send(Op.BEGIN_ACK, txId, null)
            try {
                transaction.runTx(handler)
            } catch (e: Throwable) {
                if (e == transactionEndedObject) return@launch
                println(">>>>>>>>>>>> ERROR IN TRANSACTION <<<<<<<<<<<<<<<")
                e.printStackTrace()
            }
        }
    }

    private fun receiveBeginAcknowledge(txId: Long) {
        if (txId !in beginningTransactions)
            throw Error("transaction to acknowledge [$txId] not found")
        beginningTransactions[txId]!!.resolve()
        beginningTransactions.remove(txId)
    }

    private suspend fun receiveData(txId: Long, data: String?) {
        if (txId !in transactions)
            throw Error("transaction for data [$txId] not found")
        transactions[txId]!!.receiveData(data)
    }

    private suspend fun receiveEnd(txId: Long) {
        transactions[txId]?.end()
    }


    inner class Transaction(private val txId: Long) {
        init {
            transactions[txId] = this
        }

        var ended = false
            private set

        val nextUpdateLock = ToggleLock(startLocked = true)
        val nextDataQueue = mutableListOf<String?>()

        suspend fun end() {
            ended = true
            nextUpdateLock.unlock()
            transactions.remove(txId)
        }

        suspend fun <R> runTx(handler: suspend Scope.() -> R): R {
            try {
                return coroutineScope {
                    handler(Scope(this))
                }
            } finally {
                end()
            }
        }

        suspend fun receiveData(data: String?) {
            nextDataQueue.add(data)
            nextUpdateLock.unlock()
//            println("RECEIVE HERE ${nextDataQueue.size}")
        }

        suspend inline fun nextDataRawString(): String? {
            nextUpdateLock.wait()
            if (ended && nextDataQueue.isEmpty())
                throw transactionEndedObject
            val data = nextDataQueue.removeAt(0)

            nextUpdateLock.setLocked(nextDataQueue.isEmpty())
//            println("NEXTDATA HERE ${nextDataQueue.size}")

            return data
        }

        suspend inline fun nextEmpty() {
            nextDataRawString()
        }

        suspend inline fun <reified T> nextData(): T {
            val data = nextDataRawString() ?: throw Error("transaction next expected data got none")
            return Json.decodeFromString(data)
        }


        inline suspend fun <reified T> send(data: T) {
            sendRaw(Json.encodeToString(data))
        }

        suspend fun sendRaw(data: String?) {
            if (!ended)
                send(Op.DATA, txId, data)
        }

        suspend fun sendEmpty() = sendRaw(null)

        suspend inline fun <reified T, reified R> sendReceive(sendData: T): R {
            send(sendData)
            return nextData()
        }

        inner class Scope(val innerScope: CoroutineScope) : CoroutineScope by innerScope {
            suspend inline fun <reified T> send(data: T) =
                this@Transaction.send(data)

            suspend inline fun sendString(data: String) =
                this@Transaction.sendRaw(data)

            suspend fun sendEmpty() =
                this@Transaction.sendEmpty()

            suspend inline fun <reified T, reified R> sendReceive(sendData: T): R =
                this@Transaction.sendReceive(sendData)

            suspend inline fun <reified T> nextData() =
                this@Transaction.nextData<T>()

            suspend inline fun nextString() =
                this@Transaction.nextDataRawString() ?: throw Error("was expecting non-null value received")

            suspend inline fun nextEmpty() =
                this@Transaction.nextEmpty()

            suspend inline fun <reified T> receiveIntoChannel(
                crossinline handleChannel: (ReceiveChannel<T>) -> Unit
            ) {
                val channel = Channel<T>(Channel.UNLIMITED)
                innerScope.launch { handleChannel(channel) }
                try {
                    while (nextData()) { // while has value
                        channel.send(nextData())
                        send(true)
                    }
                    send(false)
                } finally {
                    channel.close()
                }
            }

            suspend inline fun <reified T> receiveIntoChannel(
                promiseChannel: KPromise<ReceiveChannel<T>>
            ) = receiveIntoChannel {
                promiseChannel.resolve(it)
            }

            suspend inline fun <reified T> sendFromChannel(
                channel: ReceiveChannel<T>,
                crossinline handleConfirmChannel: (ReceiveChannel<Unit>) -> Unit
            ) {
                val confirmChannel = Channel<Unit>(Channel.UNLIMITED)
                innerScope.launch { handleConfirmChannel(confirmChannel) }
                innerScope.launch {
                    try {
                        while (nextData()) {
                            confirmChannel.send(Unit)
                        }
                    } finally {
                        confirmChannel.close()
                    }
                }
                for (data in channel) {
                    send(true)
                    send(data)
                }
                send(false)
            }

            suspend inline fun <reified T> sendFromChannel(
                channel: ReceiveChannel<T>,
                promiseConfirmChannel: KPromise<ReceiveChannel<Unit>>
            ) = sendFromChannel(
                channel
            ) {
                promiseConfirmChannel.resolve(it)
            }
        }
    }

    companion object {
        inline fun build(
            ws: CommonWebsocket,
            coroutineScope: CoroutineScope,
            block: KWSTransactor.() -> Unit
        ): KWSTransactor {
            val txr = KWSTransactor(ws, coroutineScope)
            block(txr)
            coroutineScope.launch {
                txr.sendHello()
            }
            return txr
        }

        inline fun <T> build(
            ws: T,
            block: KWSTransactor.() -> Unit
        ) where T : CommonWebsocket, T : CoroutineScope =
            build(ws, ws, block)
    }
}