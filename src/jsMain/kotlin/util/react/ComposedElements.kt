package util.react

import react.ChildrenBuilder

class ComposedElements<T>(val block: ChildrenBuilder.(T) -> Unit) {
    operator fun invoke(builder: ChildrenBuilder, data: T) =
        block(builder, data)
}
fun <T> childElements(block: ChildrenBuilder.(T) -> Unit) =
    ComposedElements(block)