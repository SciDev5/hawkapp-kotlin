package wsTransaction

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import util.coroutine.UntilLock

suspend inline fun <reified T> KWSTransactor.Transaction.Scope.syncSend(
    channel: ReceiveChannel<T>,
    crossinline cleanup: suspend () -> Unit
) {
    coroutineScope {
        val (finished, externalCancelJob) = UntilLock().let {
            it to launch {
                nextEmpty() // wait for cancellation from remote end
                it.unlock()
            }
        }

        val sendJob = launch {
            for (toSend in channel) {
                send(true)
                send(toSend)
            }
        }


        finished.wait()
        externalCancelJob.cancel()

        cleanup()
        sendJob.join()
    }
}

suspend inline fun <reified T> KWSTransactor.Transaction.Scope.syncReceive(
    earlyCloseSignal: UntilLock? = null,
    crossinline receive: suspend (T) -> Unit
) {
    coroutineScope {
        val earlyCloseJob = earlyCloseSignal?.let {
            launch {
                it.wait()
                sendEmpty()
            }
        }
        while (nextData()) receive(nextData())
        earlyCloseJob?.cancel()
    }
}