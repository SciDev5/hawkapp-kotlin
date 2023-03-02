package util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

interface UpdateReceiver<T> {
    fun watch(): Pair<ReceiveChannel<T>,()->Unit>
}

interface UpdateSender<T> {
    fun send(data: T)
    fun close()
}

class NonBlockingUpdates<T> : UpdateReceiver<T>, UpdateSender<T> {
    private val channels = mutableSetOf<Channel<T>>()
    private var closed = false
    private inline fun <R> throwIfClosedOrElse(block: () -> R) =
        if (closed)
            throw Error("UpdateSender is closed")
        else block()

    override fun watch(): Pair<ReceiveChannel<T>,()->Unit> = throwIfClosedOrElse {
        Channel<T>(1, BufferOverflow.DROP_OLDEST).also {
            channels.add(it)
        }.let {
            Pair(
                it
            ) {
                channels.remove(it)
                it.close()
            }
        }
    }

    override fun send(data: T) = throwIfClosedOrElse {
        for (channel in channels)
            channel.trySend(data)
    }

    override fun close() = throwIfClosedOrElse {
        closed = true
        for (channel in channels)
            channel.close()
    }

    val isWatched get() = channels.isNotEmpty()
}