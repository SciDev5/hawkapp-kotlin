object Endpoints {
    const val websocketPrimary = "/ws"

    object Auth {
        const val login = "/i"
        const val logout = "/o"
        const val signup = "/s"
    }

    object Page {
        const val main = "/"
        const val login = "/l"
    }

    const val jsMain = "/static/hawkapp.js"
    const val swMain = "/sw.js"
}