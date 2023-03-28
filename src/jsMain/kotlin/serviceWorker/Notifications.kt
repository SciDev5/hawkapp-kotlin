package serviceWorker

import data.push.NotificationHandle
import kotlinx.coroutines.await
import util.extensions.*
import wsTransaction.KWSTransactor

object Notifications {
    suspend fun requestPermissionAndSubscribe(txr: KWSTransactor) {
        when (Notification.requestPermission().await()) {
            "granted" -> subscribePush(txr)
            else -> {}
        }
    }
    val permission get() = when(Notification.permission) {
        "granted" -> Permission.GRANTED
        "denied" -> Permission.DENIED
        else -> Permission.DEFAULT
    }
    enum class Permission {
        GRANTED, DENIED, DEFAULT
    }

    suspend fun unsubscribe(txr: KWSTransactor) =
        SWRegistration.ready().pushManager.getSubscription().await()?.let {
            txr.run(NotificationHandle.MANAGE_SUBSCRIPTIONS) {
                nextEmpty()
                send<NotificationHandle.Request>(NotificationHandle.UnsubscribeRequest(it.data))
                nextEmpty()
            }
            it.unsubscribe().await()
        } ?: false
    suspend fun unsubscribeAll(txr: KWSTransactor) {
        txr.run(NotificationHandle.MANAGE_SUBSCRIPTIONS) {
            nextEmpty()
            send<NotificationHandle.Request>(NotificationHandle.UnsubscribeAllRequest)
            nextEmpty()
        }
        SWRegistration.ready().pushManager.getSubscription().await()?.unsubscribe()?.await()
    }

    private suspend fun subscribePush(txr: KWSTransactor) = txr.run(NotificationHandle.MANAGE_SUBSCRIPTIONS) {
        val applicationServerKey = nextData<ByteArray>().toArrayBuffer()

        val pushManager = SWRegistration.ready().pushManager
        val subscription =
            pushManager.getSubscription().await()
                ?: pushManager.subscribe(
                    pushSubscriptionOptions(
                        userVisibleOnly = true,
                        applicationServerKey
                    )
                ).await()
        send<NotificationHandle.Request>(NotificationHandle.SubscribeRequest(subscription.data))
        nextEmpty()
    }
}
