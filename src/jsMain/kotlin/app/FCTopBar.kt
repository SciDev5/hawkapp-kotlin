package app

import Auth
import Endpoints
import react.FC
import react.Props
import react.dom.html.ReactHTML.nav
import react.router.dom.Link

val FCTopBar = FC<Props> { _ ->
    val currentUserId = Auth.useCurrentUserId()

    nav {
        Link {
            to = Endpoints.Page.main

            + "HawkApp"
        }
        Link {
            to = Endpoints.Page.login

            if (currentUserId != null) {
                + "Logout"
            } else {
                + "Login"
            }
        }
    }
}