package app

import Auth
import Endpoints
import react.FC
import react.Props
import react.dom.html.ReactHTML.nav
import react.router.dom.Link
import style.flexChild
import style.styled

val FCTopBar = FC<Props> { _ ->
    val currentUserId = Auth.useCurrentUserId()

    styled(nav,"nav", flexChild(grow = 0.0)) {
        Link {
            to = Endpoints.Page.main

            + "HawkApp"
        }
        Link {
            to = Endpoints.Page.login

            if (currentUserId != null) {
                + "Account"
            } else {
                + "Login"
            }
        }
    }
}