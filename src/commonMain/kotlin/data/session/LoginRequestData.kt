package data.session

import kotlinx.serialization.Serializable

@Serializable
class LoginRequestData(
    val nickname: String
)