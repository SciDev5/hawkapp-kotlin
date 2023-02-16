package ws

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.*
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import wsTransaction.CommonWebsocket

class WebWebsocket(
    private val ws: WebSocket,
    private val scope: CoroutineScope,
) : CommonWebsocket(
    readyState(ws),
    setOnOpen = { handle ->
        ws.onopen = { handle() }
    },
    setOnClose = { handle ->
        ws.onclose = { e ->
            (e as CloseEvent).let {
                handle(CloseReason(it.code, it.reason))
            }
        }
    }
) {
    init {
        ws.binaryType = BinaryType.ARRAYBUFFER
        ws.onmessage = {
            val data = Uint8Array(it.data as ArrayBuffer)
            val bytes = ByteArray(data.length) { i -> data[i] }
            scope.launch {
                handles.forEach { it(bytes) }
            }
        }
    }

    override val readyState get() = readyState(ws)
    override suspend fun sendBinary(byteArray: ByteArray) {
        ws.send(Blob(arrayOf(byteArray), BlobPropertyBag()))
    }

    private val handles = mutableSetOf<suspend (ByteArray) -> Unit>()
    override fun addReceiveBinaryHandle(handle: suspend (ByteArray) -> Unit) {
        handles.add(handle)
        receiving = true
    }

    private val receiveLock = Mutex(locked = true)
    var receiving
        get() = !receiveLock.isLocked
        set(value) {
            if (value) {
                if (receiveLock.isLocked)
                    receiveLock.unlock()
            } else {
                receiveLock.tryLock()
            }
        }

    fun close(closeReason: CloseReason) = ws.close(closeReason.code,closeReason.message)
    fun close(code: Short, msg: String) = close(CloseReason(code,msg))

    companion object {
        private fun readyState(ws: WebSocket) =
            when (ws.readyState) {
                WebSocket.CONNECTING -> ReadyState.CONNECTING
                WebSocket.OPEN -> ReadyState.OPEN
                WebSocket.CLOSING -> ReadyState.CLOSED
                WebSocket.CLOSED -> ReadyState.CLOSED
                else -> throw Error("impossible to get here")
            }
    }
}

fun connectWebSocket(url: URL,scope: CoroutineScope) = WebWebsocket(WebSocket(url.toString()),scope)
fun websocketUrl(
    path: String,
    secure: Boolean = window.location.protocol.endsWith("s")
) = URL(path, window.location.href).also {
    it.protocol = if (secure) "wss" else "ws"
}