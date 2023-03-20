import app.FCTopBar
import clientData.DMZone
import clientData.User
import react.FC
import react.Props
import react.create
import react.router.dom.BrowserRouter
import react.router.useInRouterContext
import react.router.useLocation
import react.useState
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
    println(useInRouterContext())
    FCTopBar { }
    when (useLocation().pathname) {
        Endpoints.Page.main -> AppMain { }
        Endpoints.Page.login -> AppLogin { }
        else -> App404 { }
    }
}

val App = FC<Props> { _ ->
    BrowserRouter {
        children = AppRoutes.create()
    }
}