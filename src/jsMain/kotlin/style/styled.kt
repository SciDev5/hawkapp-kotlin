package style

import csstype.*
import emotion.react.css
import react.*
import react.dom.html.ReactHTML

private external interface FCBlockProps<T> : Props where
T : ChildrenBuilder,
T : PropsWithClassName {
    var cssItems: Array<out CSSCallback>
    var children: (T) -> Unit
}

private val fcsMap = mutableMapOf<ElementType<*>, FC<*>>()

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
private fun <T> ElementType<T>.styledFC() where
        T : ChildrenBuilder,
        T : PropsWithClassName =
    (fcsMap[this] ?: initializeStyledFC().also {
        fcsMap[this] = it
    }) as FC<FCBlockProps<T>>

private fun <T> ElementType<T>.initializeStyledFC() where
        T : ChildrenBuilder,
        T : PropsWithClassName = FC<FCBlockProps<T>> { props ->
    this@initializeStyledFC {
        css {
            for (c in props.cssItems) {
                c()
            }
        }
        props.children(this)
    }
}

typealias CSSCallback = PropertiesBuilder.() -> Unit

fun ChildrenBuilder.styledDiv(
    key: String,
    vararg cssItems: CSSCallback,
    block: ChildrenBuilder.() -> Unit
) = styled(ReactHTML.div, key, *cssItems) { block() }

fun <T> ChildrenBuilder.styled(
    elt: ElementType<T>,
    key: String,
    vararg cssItems: CSSCallback,
    block: T.() -> Unit
) where
        T : ChildrenBuilder,
        T : PropsWithClassName {
    (elt.styledFC()) {
        this.key = key
        this.cssItems = cssItems
        this.children = block
    }
}