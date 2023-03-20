import react.FC
import react.Props
import react.dom.html.ReactHTML

external interface AppDisconnectedProps : Props {
    var refresh: () -> Unit
}

val AppDisconnected = FC<AppDisconnectedProps> { props ->
    ReactHTML.button {
        onClick = { props.refresh() }

        + "reload"
    }
}