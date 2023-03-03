package util.react

import react.createContext
import react.useContext
import wsTransaction.KWSTransactor

private val ctx = createContext<KWSTransactor?>(null)

val ReactTXRProvider = ctx.Provider

fun useTXROrNull() = useContext(ctx)
fun useTXR() = useTXROrNull() ?: throw Error("KWSTransactor not in context")