package util.react

import data.TimestampedId
import react.createContext
import react.useContext

private val ctx = createContext<TimestampedId?>(null)

val ReactUserIdProvider = ctx.Provider

fun useUserIdOrNull() = useContext(ctx)
fun useUserId() = useUserIdOrNull() ?: throw Error("User not in context")