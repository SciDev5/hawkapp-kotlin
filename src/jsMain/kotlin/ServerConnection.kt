import data.TimestampedId
import kotlinx.coroutines.launch
import react.*
import util.react.ComposedElements
import util.react.ReactTXRProvider
import util.react.useCoroutineScope
import ws.connectWebSocket
import ws.websocketUrl
import wsTransaction.KWSTransactor


external interface ServerConnectionProps : Props {
    var userId: TimestampedId?
    var children: ComposedElements<ServerConnectionChildrenData>

}

interface ServerConnectionChildrenData
class SCDataConnected() : ServerConnectionChildrenData
class SCDataDisconnected(val failed: Boolean) : ServerConnectionChildrenData

val ServerConnection = FC<ServerConnectionProps> { props ->
    var txr by useState<KWSTransactor?>(null)
    var failed by useState(false)

    val scope = useCoroutineScope()

    useEffect(props.userId?.v, scope) {
        var connectionEnded = false
        val ws = connectWebSocket(websocketUrl(Endpoints.websocketPrimary), scope)
        failed = false
        val newTxr = KWSTransactor.build(ws, scope) {

        }
        scope.launch {
            failed = ws.failed()
            if (failed) return@launch
            val helloJob = launch {
                newTxr.waitForHello()

                txr = newTxr
            }
            ws.waitClosed()
            helloJob.cancel()
            if (!connectionEnded) txr = null
        }
        cleanup {
            ws.close(1000, "ending")
            txr = null
            connectionEnded = true
        }
    }

    ReactTXRProvider(txr) {
        props.children(
            this,
            if (txr == null)
                SCDataDisconnected(
                    failed = failed
                )
            else
                SCDataConnected()
        )
    }
}