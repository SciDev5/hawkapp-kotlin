package app

import Auth
import Endpoints
import csstype.Display
import csstype.FontWeight
import csstype.TextDecoration
import csstype.em
import react.FC
import react.Props
import react.dom.html.ReactHTML.nav
import react.router.dom.Link
import style.*

private fun navStyles(n: Int, em: Boolean = false): CSSCallback = {
    fontWeight = if (em) FontWeight.bold else FontWeight.normal
    val (fg, bg) = StyleColors.colorful[n]
    background = StyleColors.transparent
    color = StyleColors.fgMain

    transition = StyleColors.bgFgTransition
    display = Display.inlineBlock
    textDecoration = cssNone<TextDecoration>()

    hover {
        background = bg
        color = fg
    }

    paddingInline = 1.0.em
    paddingBlock = 0.3.em
}

val FCNav = FC<Props> { _ ->
    val currentUserId = Auth.useCurrentUserId()

    styled(nav, "nav", flexChild(grow = 0.0)) {
        styled(Link, ">m", navStyles(0, true)) {
            to = Endpoints.Page.main

            +"HawkApp"
        }
        styled(Link, ">l", navStyles(if (currentUserId != null) 1 else 2)) {
            to = Endpoints.Page.login

            if (currentUserId != null) {
                +"Account"
            } else {
                +"Login"
            }
        }
    }
}