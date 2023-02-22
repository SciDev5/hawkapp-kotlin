package data.channel

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
data class ChannelLookupData(
    val chId: TimestampedId
)