package util.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

@OptIn(DelicateCoroutinesApi::class)
class MultiReceiverChannel<T> private constructor(
    private val channel: Channel<T>
) : SendChannel<T> by channel {
    private val receivers = mutableSetOf<Channel<T>>()

    constructor() : this(
        Channel()
    )

    init {
        GlobalScope.launch {
            pipeToReceivers()
        }
    }

    private suspend fun pipeToReceivers() {
        for (data in channel)
            receivers.forEach { it.send(data) }
        receivers.forEach { it.close() }
    }

    override fun close(cause: Throwable?) =
        channel.close(cause).also {
            println("CLOSING MULTIRECEIVER")
            receivers.forEach { it.close(cause) }
        }

    inner class Receiver private constructor(
        private val receiverChannel: Channel<T>
    ) : ReceiveChannel<T> by receiverChannel {
        constructor() : this(Channel())

        init {
            receivers.add(receiverChannel)
        }

        fun closeReceiving() {
            receiverChannel.close()
            receivers.remove(receiverChannel)
        }
    }
}
