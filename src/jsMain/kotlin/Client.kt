import kotlinx.browser.document
import react.create
import react.dom.client.createRoot

fun main() {

    val container = document.createElement("div")
    document.body!!.appendChild(container)

    val welcome = AppMain.create {
        sessionId = byteArrayOf(1,2,3,4,5)
    }

    createRoot(container).render(welcome)
}