package util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import util.coroutine.UntilLock

interface UpdateReceiver<T> {
    fun watch(): Pair<ReceiveChannel<T>,()->Unit>
    fun watch(untilLock: UntilLock): ReceiveChannel<T>
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
            ) { closeChannel(it) }
        }
    }

    private fun closeChannel(channel: Channel<T>) {
        channels.remove(channel)
        channel.close()
    }
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override fun watch(untilLock: UntilLock) =
        Channel<T>(1, BufferOverflow.DROP_OLDEST).also {
            val endJob = GlobalScope.launch {
                untilLock.wait()
                closeChannel(it)
            }
            it.invokeOnClose { _ ->
                if (endJob.isActive)
                    endJob.cancel()
                closeChannel(it)
            }
            channels.add(it)
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