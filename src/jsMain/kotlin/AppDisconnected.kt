import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.router.useNavigate

external interface AppDisconnectedProps : Props {
    var refresh: () -> Unit
    var loggedOut: Boolean
}

val AppDisconnected = FC<AppDisconnectedProps> { props ->
    val redir = useNavigate()
    if (props.loggedOut)
        redir("/l")

    ReactHTML.button {
        onClick = { props.refresh() }

        + "reload"
    }
}