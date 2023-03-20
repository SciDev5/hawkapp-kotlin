import react.FC
import react.Props
import react.router.dom.Link


val AppLoggedOut = FC<Props> { props ->
    + "ur logged out! "
    Link {
        to = Endpoints.Page.login

        + "login"
    }
}