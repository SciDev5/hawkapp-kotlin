package data.channel

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
class ChannelMessageData(
    val sender:TimestampedId,
    val msgId:TimestampedId,
    val content:Content
) {
    @Serializable
    class Content(
        val t: String
    )
}