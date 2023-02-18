import data.session.LoginRequestData
import data.session.UserSessionData
import kotlinx.browser.window
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.input
import react.useEffect
import react.useState
import util.Fetch
import util.react.childElements
import util.react.suspendCallback
import util.react.useCoroutineScope
import wsTransaction.KWSTransactor
import wsTransaction.WSTransactionBiChannel
import wsTransaction.runChannelUntilClose
import kotlin.random.Random

external interface KProps : Props {
    var txr: KWSTransactor
}


val K = FC<KProps> { props ->
    var txt by useState("-")
    var chan by useState<WSTransactionBiChannel<TestMessage, TestMessage>?>(null)
    var behind by useState(0)

    val txr = props.txr
    val scope = useCoroutineScope()
    useEffect(txr) {
        val biChan = WSTransactionBiChannel<TestMessage, TestMessage>()
        chan = biChan
        scope.launch {
            launch {
                txr.run("h") {
                    runChannelUntilClose(biChan)
                }
            }
            launch {
                for (k in biChan.channelNumUnconfirmedSent) {
                    behind = k
                }
            }
            var txt2 = txt
            for (data in biChan) {
                txt2 += " [${data.t}]"
                txt = txt2
            }
            txt2 += "<END>"
            txt = txt2
        }
    }
    var inpText by useState("")
    if (chan != null) {
        ReactHTML.div {
            input {
                value = inpText
                onChange = {
                    inpText = it.currentTarget.value
                }
            }
            button {
                onClick = {
                    scope.launch {
                        chan!!.send(TestMessage(t = inpText))
                    }
                }
                +"send"
            }
        }
    }
    +"$txt [$behind]"
}

val AppMain = FC<Props> { _ ->
    val s = useCoroutineScope()
    var wsRefreshHash by useState(0)

    button {
        onClick = suspendCallback(s) {
            val name = window.prompt("Nickname?")
                ?: return@suspendCallback
            Fetch.post<LoginRequestData, UserSessionData>(
                "/auth/login",
                LoginRequestData(name)
            )
            wsRefreshHash = Random.nextInt()
        }
        +"login"
    }
    button {
        onClick = suspendCallback(s) {
            Fetch.post<Boolean>("/auth/logout")
            wsRefreshHash = Random.nextInt()
        }
        +"logout"
    }
    button {
        onClick = suspendCallback(s) {
            println(Fetch.get<TestMessage>("/test"))
        }
    }

    println("RE-RENDER#$wsRefreshHash")


    ServerConnection {
        refreshHash = wsRefreshHash.hashCode()
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