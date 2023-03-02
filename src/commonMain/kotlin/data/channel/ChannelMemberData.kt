package data.channel

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
class ChannelMemberData(
    val id: TimestampedId,
    val writeAccess: Boolean
) : Comparable<ChannelMemberData> {
    override fun compareTo(other: ChannelMemberData) =
        this.id compareTo other.id
}