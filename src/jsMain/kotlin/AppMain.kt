import data.user.UserData
import kotlinx.browser.window
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import util.react.childElements
import util.react.suspendCallback
import util.react.useCoroutineScope


val AppMain = FC<Props> { _ ->
    val s = useCoroutineScope()
    val userId = Auth.useCurrentUserId()

    if (userId == null) {
        button {
            onClick = suspendCallback(s) {
                val username = window.prompt("Username?") ?: return@suspendCallback
                val password = window.prompt("Password?") ?: return@suspendCallback
                Auth.login(username, password)
            }
            +"login"
        }
        button {
            onClick = suspendCallback(s) {
                val username = window.prompt("Username?") ?: return@suspendCallback
                val password = window.prompt("Password?") ?: return@suspendCallback
                Auth.signup(UserData.Creation(username, password))
            }
            +"signup"
        }
    } else {
        button {
            onClick = suspendCallback(s) {
                Auth.logout()
            }
            +"logout"
        }
    }

    div {
        +"user id: ${userId?.v?.toString(16) ?: "<NULL>"}"
    }


    ServerConnection {
        this.userId = userId
        children = childElements {
            if (it is SCDataConnected) {
                +"CONNECTED"
                AppConnected { }
            }
            if (it is SCDataDisconnected) {
                +"DISCONNECTED"
                if (it.failed)
                    +" [failed]"
            }
        }
    }
}