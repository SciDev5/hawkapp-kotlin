import app.FCTopBar
import clientData.DMZone
import clientData.User
import csstype.vh
import react.FC
import react.Props
import react.create
import react.router.dom.BrowserRouter
import react.router.useLocation
import react.useState
import style.flexChild
import style.flexContainerVertical
import style.flexDividerHorizontal
import style.styledDiv
import util.react.childElements
import util.withTxr
import kotlin.random.Random


val AppMain = FC<Props> { _ ->
    val userId = Auth.useCurrentUserId()
    var retry by useState(0)

    ServerConnection {
        this.userId = userId
        children = childElements {
            if (it is SCDataConnected) {
                User.Instances.withTxr(it.txr)
                DMZone.Instances.withTxr(it.txr)
                AppConnected { }
            }
            if (it is SCDataDisconnected) {
                if (it.failed) {
                    AppDisconnected {
                        this.refresh = {
                            retry = Random.nextInt()
                        }
                    }
                } else {
                    AppConnecting { }
                }
            }
        }
    }
}


private val AppRoutes = FC<Props> {
    val user = Auth.useCurrentUserId()
    styledDiv("_", flexContainerVertical(), { height = 100.0.vh }) {
        FCTopBar { }
        flexDividerHorizontal(0)
        styledDiv("content", flexChild(), flexContainerVertical()) {
            when (useLocation().pathname) {
                Endpoints.Page.main -> if (user != null) AppMain { } else AppLoggedOut { }
                Endpoints.Page.login -> AppLogin { }
                else -> App404 { }
            }
        }
    }
}

val App = FC<Props> { _ ->
    BrowserRouter {
        children = AppRoutes.create()
    }
}