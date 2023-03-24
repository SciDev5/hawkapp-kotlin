import react.FC
import react.Props
import react.dom.html.ReactHTML
import style.cssTextCentered
import style.styled

val AppConnecting = FC<Props> {
    styled(ReactHTML.p,"p", cssTextCentered) {
        + "... connecting ..."
    }
}