import channel.DMsPanel
import react.FC
import react.Props
import wsTransaction.KWSTransactor

external interface AppConnectedProps : Props {
    var txr: KWSTransactor
}


val AppConnected = FC<AppConnectedProps> { props ->
    DMsPanel {
        txr = props.txr
    }
}