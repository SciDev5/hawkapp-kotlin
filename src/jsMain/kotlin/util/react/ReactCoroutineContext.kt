package util.react

import kotlinx.coroutines.MainScope
import react.createContext
import react.useContext

private val ctx = createContext(MainScope())

val ReactCoroutineProvider = ctx.Provider

fun useCoroutineScope() = useContext(ctx)