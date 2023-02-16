package wsTransaction

import util.coroutine.KPromise
import util.coroutine.resolve

abstract class CommonWebsocket(
    initReadyState: ReadyState,
    setOnOpen: (handle: () -> Unit) -> Unit,
    setOnClose: (handle: (CloseReason) -> Unit) -> Unit
) {
    abstract suspend fun sendBinary(byteArray: ByteArray)
    abstract fun addReceiveBinaryHandle(handle: suspend (ByteArray) -> Unit)
    abstract val readyState: ReadyState

    enum class ReadyState {
        CONNECTING, OPEN, CLOSED
    }

    class CloseReason(val code: Short, val message: String)

    val open get() = readyState == ReadyState.OPEN
    private val promiseOpenOrClosed = KPromise<Unit>()
    suspend fun waitOpenOrClose() = promiseOpenOrClosed.await()
    private val promiseClosed = KPromise<CloseReason>()
    suspend fun waitClosed() = promiseClosed.await()

    private fun markOpen() {
        promiseOpenOrClosed.resolve()
    }

    suspend fun failed(): Boolean {
        promiseOpenOrClosed.await()
        return !open
    }

    fun markClosed(closeReason: CloseReason) {
        promiseOpenOrClosed.resolve()
        promiseClosed.resolve(closeReason)
    }

    init {
        when (initReadyState) {
            ReadyState.CONNECTING -> {}
            ReadyState.OPEN -> markOpen()
            ReadyState.CLOSED -> markClosed(CloseReason(-1, ""))
        }
        if (initReadyState == ReadyState.CONNECTING)
            setOnOpen { markOpen() }
        if (initReadyState == ReadyState.OPEN || initReadyState == ReadyState.CONNECTING)
            setOnClose { closeReason -> markClosed(closeReason) }
    }
}