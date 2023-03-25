package data.push

import kotlinx.serialization.Serializable

@Serializable
data class PushSubscriptionInfoKT(
    val endpoint: String,
    val p256dh: String,
    val auth: String,
)