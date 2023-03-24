import react.FC
import react.Props
import react.dom.html.ReactHTML
import style.cssTextCentered
import style.styled

val App404 = FC<Props> {
    styled(ReactHTML.h1,"h", cssTextCentered) {
        + "404 Not found"
    }
    styled(ReactHTML.p,"p", cssTextCentered) {
        + "Could not find requested page lol."
    }
}