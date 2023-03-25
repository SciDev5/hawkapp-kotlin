package me.scidev5.application.push

import data.push.NotificationHandle
import me.scidev5.application.User
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Utils
import org.bouncycastle.jce.interfaces.ECPublicKey
import wsTransaction.KWSTransactionHandle
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

fun Notification.send() {
    Notifications.sender.send(this)
}
object Notifications {
    private val keypair = KeyPairGenerator.getInstance("ECDH").also {
        it.initialize(ECGenParameterSpec(PushService.SERVER_KEY_CURVE))
    }.genKeyPair()
    val sender = PushService(keypair)
    val pk: ByteArray = Utils.encode(keypair.public as ECPublicKey)

    fun test(user: User) {
        user.sendNotification("""{"title":"test lol","tag":"${Random.nextInt()}"}""")
    }

    object Handle {
        fun manage(user: User) = KWSTransactionHandle(
            NotificationHandle.MANAGE_SUBSCRIPTIONS
        ) {
            send(pk)
            with(user.subscriptions) {
                // TODO: add / remove in database
                when (val req = nextData<NotificationHandle.Request>()) {
                    is NotificationHandle.SubscribeRequest -> add(req.sub)
                    is NotificationHandle.UnsubscribeRequest -> remove(req.sub)
                    is NotificationHandle.UnsubscribeAllRequest -> clear()
                }
            }
            sendEmpty()
        }
    }
}