package data.push

import kotlinx.serialization.Serializable

object NotificationHandle {
    @Serializable
    sealed interface Request
    @Serializable
    class SubscribeRequest(val sub: PushSubscriptionInfoKT) : Request
    @Serializable
    class UnsubscribeRequest(val sub: PushSubscriptionInfoKT) : Request
    @Serializable
    object UnsubscribeAllRequest : Request

    const val MANAGE_SUBSCRIPTIONS = "p--"
}