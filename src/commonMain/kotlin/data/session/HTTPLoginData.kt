package data.session

import data.TimestampedId
import data.user.UserData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import util.TextValidator

class HTTPLoginData {
    @Serializable
    data class ReqSignup(val data: UserData.Creation) {
        fun cleaned() = ReqSignup(UserData.Creation(
            username = data.username.trim(),
            password = data.password.trim()
        ))
    }

    @Serializable
    sealed interface ResSignup

    @Serializable
    @SerialName("fail")
    class ResSignupFailure(val reason: Reason, vararg val validationFailures: TextValidator.Failure) : ResSignup {
        @Serializable
        enum class Reason {
            USERNAME_TAKEN,
            USERNAME_INVALID,
            PASSWORD_INVALID
        }
    }

    @Serializable
    data class ReqLogin(val username: String, val password: String) {
        fun cleaned() = ReqLogin(
            username = username.trim(),
            password = password.trim()
        )
    }

    @Serializable
    sealed interface ResLogin


    @Serializable
    @SerialName("fail")
    data class ResLoginFailure(val reason: Reason) : ResLogin {
        @Serializable
        enum class Reason {
            NO_SUCH_USER,
            BAD_CREDENTIALS
        }
    }

    @Serializable
    class ResLogout(val wasChange: Boolean)

    @Serializable
    @SerialName("ok")
    class ResSuccess(val userId: TimestampedId) : ResLogin, ResSignup
}