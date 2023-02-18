package data.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class HTTPLoginData {
    @Serializable
    class ReqLogin(val username: String)

    @Serializable
    sealed interface ResLogin

    @Serializable
    @SerialName("ok")
    class ResLoginSuccess : ResLogin

    @Serializable
    @SerialName("fail")
    class ResLoginFailure(val reason: Reason) : ResLogin {
        @Serializable
        enum class Reason {
            BAD_CREDENTIALS
        }
    }

    @Serializable
    class ResLogout(val wasChange: Boolean)
}