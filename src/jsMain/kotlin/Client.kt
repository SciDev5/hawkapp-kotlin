import csstype.em
import emotion.css.ClassName
import kotlinx.browser.document
import kotlinx.browser.window
import react.create
import react.dom.client.createRoot
import style.StyleColors
import style.getSansFont

fun main() {
    if (js("'serviceWorker' in navigator").unsafeCast<Boolean>())
        window.navigator.serviceWorker.register(Endpoints.swMain)

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