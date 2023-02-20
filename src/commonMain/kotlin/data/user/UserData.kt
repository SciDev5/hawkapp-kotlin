package data.user

import data.TimestampedId
import kotlinx.serialization.Serializable
import util.TextValidator

@Serializable
data class UserData(
    val id: TimestampedId,
    val username: String
) {
    @Serializable
    data class Creation(
        val username: String,
        val password: String
    )
    object Validate {
        val username = TextValidator {
            lengthInRange(4 .. 32)
            noBoundingWhitespace()
        }
        val password = TextValidator {
            lengthInRange(8 ..64)
            noBoundingWhitespace()
        }
    }
}