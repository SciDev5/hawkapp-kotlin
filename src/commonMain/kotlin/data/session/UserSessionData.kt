package data.session

import kotlinx.serialization.Serializable

@Serializable
class UserSessionData(
    val nickname: String
) { // TODO: accounts
    companion object {
        const val COOKIE_NAME = "userSession"
    }

    override fun hashCode() = nickname.hashCode()
    override fun equals(other: Any?) =
        other is UserSessionData && other.nickname == this.nickname
}