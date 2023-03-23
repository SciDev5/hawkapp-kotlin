import csstype.em
import emotion.css.ClassName
import kotlinx.browser.document
import react.create
import react.dom.client.createRoot
import style.StyleColors
import style.getSansFont

fun main() {
    document.body!!.className += " ${ClassName {
        fontFamily = getSansFont()
        margin = 0.0.em
        StyleColors.cssBGMain(this)
        StyleColors.cssFGMain(this)
    }}"

    val container = document.createElement("div")
    document.body!!.appendChild(container)

    val welcome = App.create { }

    createRoot(container).render(welcome)
}