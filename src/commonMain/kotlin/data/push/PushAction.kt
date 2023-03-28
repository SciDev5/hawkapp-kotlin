package data.push

import kotlinx.serialization.Serializable

@Serializable
data class PushAction(val action: String, val title: String)