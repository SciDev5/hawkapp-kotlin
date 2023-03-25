package util.extensions

import data.push.PushSubscriptionInfoKT
import kotlinx.js.Record
import org.khronos.webgl.ArrayBuffer
import org.w3c.workers.ServiceWorkerRegistration
import kotlin.js.Promise


external object Notification {
    /** Requests permission for showing notifications.
     * @return "granted", "denied", or "default"
     */
    fun requestPermission(): Promise<String>
}

external interface PushSubscription {
    var endpoint: String
    var expirationTime: Double
    var options : PushSubscriptionOptions
    //    ArrayBuffer? getKey(PushEncryptionKeyName name);
    fun unsubscribe(): Promise<Boolean>

    fun toJSON(): PushSubscriptionJSON
}

external interface PushSubscriptionJSON {
    var endpoint: String
    var expirationTime: Double?
    var keys: Record<String, String>
}
val PushSubscription.data get() = toJSON().let {
    PushSubscriptionInfoKT(
        endpoint = it.endpoint,
        p256dh = it.keys["p256dh"] ?: throw Error("'p256dh' key missing"),
        auth = it.keys["auth"] ?: throw Error("'auth' key missing")
    )
}
external interface PushSubscriptionOptions {
    var userVisibleOnly: Boolean
    var applicationServerKey: ArrayBuffer
}
fun pushSubscriptionOptions(
    userVisibleOnly: Boolean,
    applicationServerKey: ArrayBuffer,
) = js("{}").unsafeCast<PushSubscriptionOptions>().also {
    it.userVisibleOnly = userVisibleOnly
    it.applicationServerKey = applicationServerKey
}
external interface PushManager {
    fun subscribe(options: PushSubscriptionOptions): Promise<PushSubscription>
    fun getSubscription(): Promise<PushSubscription?>
}
inline val ServiceWorkerRegistration.pushManager
    get() = asDynamic().pushManager.unsafeCast<PushManager>()
