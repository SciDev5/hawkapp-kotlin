package wsTransaction

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WSTransactionBiChannel<In, Out> private constructor(
    private val parseIn: (String) -> In,
    private val serializeOut: (Out) -> String,
    private val channelOut: Channel<Out>,
    private val channelIn: Channel<In>
) : SendChannel<Out> by channelOut, ReceiveChannel<In> by channelIn {
    constructor(
        bufferSize: Int,
        bufferOverflow: BufferOverflow,
        parsePayloadIn: (String) -> In,
        serializePayloadOut: (Out) -> String,
    ) : this(
        parsePayloadIn,
        serializePayloadOut,
        Channel(bufferSize, bufferOverflow),
        Channel(bufferSize, bufferOverflow),
    )

    @Serializable
    enum class PacketType {
        DATA, ACKNOWLEDGEMENT, CLOSE
    }

    private val channelNumUnconfirmedSentInternal = Channel<Int>(1, BufferOverflow.DROP_OLDEST)
    val channelNumUnconfirmedSent: ReceiveChannel<Int> = channelNumUnconfirmedSentInternal
    private var numUnconfirmedSent = 0


    private val channelControlsOut = Channel<PacketType>(Channel.RENDEZVOUS)

    private val sendOrderMutex = Mutex()

    private var linked = false
    private var closed = false
    suspend fun runUntilClosed(transaction: KWSTransactor.Transaction.Scope) {
        if (linked)
            throw IllegalStateException("Already linked, cannot double link.")
        linked = true

        coroutineScope {
            launch {
                while (!closed) {
                    when (transaction.nextData<PacketType>()) {
                        PacketType.CLOSE -> {
                            closeChannels()
                        }
                        PacketType.DATA -> {
                            channelIn.send(parseIn(transaction.nextString()))
                            sendOrderMutex.withLock {
                                transaction.send(PacketType.ACKNOWLEDGEMENT)
                            }
                        }
                        PacketType.ACKNOWLEDGEMENT -> {
                            numUnconfirmedSent--
                            channelNumUnconfirmedSentInternal.send(numUnconfirmedSent)
                        }
                    }
                }
            }
            launch {
                for (toSend in channelOut) {
                    numUnconfirmedSent++
                    channelNumUnconfirmedSentInternal.send(numUnconfirmedSent)
                    sendOrderMutex.withLock {
                        transaction.send(PacketType.DATA)
                        transaction.sendString(serializeOut(toSend))
                    }
                }
            }
            launch {
                for (toSend in channelControlsOut) {
                    sendOrderMutex.withLock {
                        transaction.send(toSend)
                    }
                }
            }
        }
    }

    private fun closeChannels() {
        closed = true

        channelIn.close()
        channelOut.close()
        channelControlsOut.close()
        channelNumUnconfirmedSentInternal.close()
    }

    suspend fun close() {
        if (closed) return // do not close twice

        channelControlsOut.send(PacketType.CLOSE)
        closeChannels()
    }


    companion object {
        inline operator fun <reified In, reified Out> invoke(bufferSize: Int = Channel.RENDEZVOUS) =
            WSTransactionBiChannel<In, Out>(
                bufferSize,
                BufferOverflow.SUSPEND,
                { Json.decodeFromString(it) },
                { Json.encodeToString(it) }
            )
    }
}

suspend fun <I, O> KWSTransactor.Transaction.Scope.runChannelUntilClose(channel: WSTransactionBiChannel<I, O>) =
    channel.runUntilClosed(this)
