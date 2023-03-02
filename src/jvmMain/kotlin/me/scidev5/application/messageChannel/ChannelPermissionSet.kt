package me.scidev5.application.messageChannel

import me.scidev5.application.User

interface ChannelPermissionSet {
    operator fun get(user: User): ChannelPermissions
}
data class ChannelPermissions(
    val use: Boolean = true,
    val readHistory: Boolean = use,
    val write: Boolean = use
)