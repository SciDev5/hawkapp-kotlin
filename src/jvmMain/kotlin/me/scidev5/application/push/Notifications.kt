package me.scidev5.application.push

import data.push.NotificationData
import data.push.NotificationHandle
import data.push.PushAction
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.scidev5.application.User
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Utils
import org.bouncycastle.jce.interfaces.ECPublicKey
import wsTransaction.KWSTransactionHandle
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

suspend fun NotificationData.sendTo(vararg users: User) {
    Notifications.send(this, *users)
}
@OptIn(DelicateCoroutinesApi::class)
object Notifications {
    private val keypair = KeyPairGenerator.getInstance("ECDH").also {
        it.initialize(ECGenParameterSpec(PushService.SERVER_KEY_CURVE))
    }.genKeyPair()
    private val pushService = PushService(keypair)
    val pk: ByteArray = Utils.encode(keypair.public as ECPublicKey)
    private val pushChannel = Channel<Notification>(Channel.UNLIMITED)

    suspend fun test(user: User) {
        NotificationData(
            "test",
            "content",
            "#${Random.nextInt()}",
            PushAction("hello","HELLO ACTION")
        ).sendTo(user)
    }
    suspend fun send(notificationData: NotificationData, vararg user: User) {
        val content = Json.encodeToString(notificationData)
        val subs = user.flatMap { it.pushSubscriptions }.toSet().map { it.convert() }
        for (sub in subs) {
            pushChannel.send(Notification(sub, content))
        }
    }
    init {
        GlobalScope.launch {
            for (notificationToSend in pushChannel)
                pushService.send(notificationToSend)
        }
    }

    object Handle {
        fun manage(user: User) = KWSTransactionHandle(
            NotificationHandle.MANAGE_SUBSCRIPTIONS
        ) {
            send(pk)
            with(user.pushSubscriptions) {
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