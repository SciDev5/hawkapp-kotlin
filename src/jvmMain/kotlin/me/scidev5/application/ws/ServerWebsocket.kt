package me.scidev5.application.ws

import wsTransaction.CommonWebsocket
import util.coroutine.UntilLock
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ServerWebsocket(
    private val wsSession: DefaultWebSocketServerSession,
) : CommonWebsocket(
    ReadyState.OPEN,
    setOnOpen = { },
    setOnClose = { }
), DefaultWebSocketSession by wsSession {
    val sendChannel = Channel<ByteArray>(Channel.RENDEZVOUS)
    override suspend fun sendBinary(byteArray: ByteArray) {
        sendChannel.send(byteArray)
    }

    suspend inline fun sendOutgoingFrames() {
        for (frame in sendChannel)
            send(frame)
    }

    private val handles = mutableSetOf<suspend (ByteArray) -> Unit>()
    override fun addReceiveBinaryHandle(handle: suspend (ByteArray) -> Unit) {
        handles.add(handle)
        receiveLock.unlock()
    }

    private val receiveLock = UntilLock()

    private var closed = false
    override val readyState: ReadyState
        get() = if (closed) ReadyState.CLOSED else ReadyState.OPEN

    suspend fun init() {
        launch {
            for (frame in incoming) {
                receiveLock.wait()
                frame as? Frame.Binary ?: continue

                for (handle in handles)
                    handle(frame.data)
            }
            markClosed(closeReason.await()?.let {
                CloseReason(it.code, it.message)
            } ?: CloseReason(-1, ""))
            closed = true
        }
    }
}

suspend fun DefaultWebSocketServerSession.websocketObject() =
    ServerWebsocket(this).also { it.init() }