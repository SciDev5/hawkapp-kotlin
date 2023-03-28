package data.push

import kotlinx.serialization.Serializable

@Serializable
class NotificationData(
    val title: String,
    val body: String,
    val tag: String,
    vararg val actions: PushAction,
)