package data.channel

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
class ChannelData(
    val id: TimestampedId,
    val name: String,
    val members: Array<ChannelMemberData>
) {

    object TransactionNames {
        private const val BASE = "ch"
        const val CHANNEL_LISTEN = "$BASE l"
        const val READ_HISTORY = "$BASE rh"
    }
    /*
     * Design Requirements:
     * - Can send messages, and they arrive in a timely manner
     * - Members can only read/write with permission
     * Nice to have:
     * - Editing/deleting
     * - Content filtering
     * - Rich message contents
     */
}