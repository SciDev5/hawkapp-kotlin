import channel.DMsPanel
import react.FC
import react.Props
import react.dom.html.ReactHTML
import serviceWorker.Notifications
import util.react.suspendCallback
import util.react.useCoroutineScope
import util.react.useTXR

val AppConnected = FC<Props> { _ ->
    DMsPanel { }

    val s = useCoroutineScope()
    val txr = useTXR()

    ReactHTML.button {
        onClick = suspendCallback(s) {
            Notifications.unsubscribe(txr)
            Notifications.requestPermissionAndSubscribe(txr)
        }
        + "push sub test"
    }
}