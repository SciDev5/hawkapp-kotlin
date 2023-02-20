package data.session

import data.TimestampedId
import kotlinx.serialization.Serializable

@Serializable
data class UserSessionData(
    val userId: TimestampedId
) {
    companion object {
        const val COOKIE_NAME = "userSession"
    }
}