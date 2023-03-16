package data.channel

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
class DMZoneData(
    val id: TimestampedId,
    val members: Array<out ChannelMemberData>
) {
    object TransactionNames {
        const val WATCH_LIST = "dzw"
        const val CREATE = "dzc"
        const val LEAVE = "dzx"
        const val SYNC = "dzs"
        const val GET = "dzg"
    }
}