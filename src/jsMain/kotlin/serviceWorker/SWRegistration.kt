package serviceWorker

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.workers.ServiceWorkerRegistration

object SWRegistration {
    fun get(block: ServiceWorkerRegistration.()->Unit) =
        window.navigator.serviceWorker.ready.then(block)

    suspend fun ready() = window.navigator.serviceWorker.ready.await()
}