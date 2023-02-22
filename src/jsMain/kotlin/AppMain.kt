import channel.MessageChannel
import data.TimestampedId
import data.channel.ChannelLookupData
import data.user.UserData
import kotlinx.browser.window
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import util.react.childElements
import util.react.suspendCallback
import util.react.useCoroutineScope
import wsTransaction.KWSTransactor

external interface KProps : Props {
    var txr: KWSTransactor
}


val K = FC<KProps> { props ->
    MessageChannel {
        txr = props.txr
        channelLookup = ChannelLookupData(TimestampedId(0,0))
    }
}

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
                K {
                    txr = it.txr
                }
            }
            if (it is SCDataDisconnected) {
                +"DISCONNECTED"
                if (it.failed)
                    +" [failed]"
            }
        }
    }
}