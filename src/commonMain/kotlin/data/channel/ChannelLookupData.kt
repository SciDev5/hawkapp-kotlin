package data.channel

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
sealed interface ChannelLookupData {
    @Serializable
    data class DM(
        val id: TimestampedId
    ): ChannelLookupData
}