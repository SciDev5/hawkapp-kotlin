package util.extensions

import react.FC
import react.Props
import react.ReactNode

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
val <P: Props> FC<P>.reactNode get() =
    this as ReactNode
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
val <T> List<T>.reactNode get() =
    this as ReactNode