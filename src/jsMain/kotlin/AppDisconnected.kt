import csstype.em
import react.FC
import react.Props
import react.dom.html.ReactHTML
import style.InputStyle
import style.cssTextCentered
import style.styled

external interface AppDisconnectedProps : Props {
    var refresh: () -> Unit
}

val AppDisconnected = FC<AppDisconnectedProps> { props ->

    styled(ReactHTML.h1,"h", cssTextCentered) {
        + "Server unreachable ¯\\_(ツ)_/¯"
    }
    styled(ReactHTML.button, "b", InputStyle.lined, {
        marginInline = 5.0.em
        marginBlock = 1.0.em
        padding = 1.0.em
    }) {
        onClick = { props.refresh() }

        + "reconnect"
    }
}