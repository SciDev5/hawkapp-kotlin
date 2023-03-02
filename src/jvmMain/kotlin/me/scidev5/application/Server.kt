package me.scidev5.application

import Endpoints
import data.session.UserSessionData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.html.*
import me.scidev5.application.util.Env
import me.scidev5.application.ws.websocketObject

fun HTML.index() {
    head {
        title("HawkApp")
        link(rel = "icon", type = "image/x-icon", href = "/static/favicon.ico")
    }
    body {
        div {
            id = "root"
        }
        script(src = "/static/hawkapp.js") {}
        noScript {
            +"This app requires JavaScript, enable it or it'll keep not working"
        }
    }
}

fun HTML.notFoundPage() {
    head {
        title("HawkApp - 404")
    }
    body {
        +"404 lol"
    }
}


fun main() {
    databaseInit()

    embeddedServer(Netty, port = 8080, host = "127.0.0.1", watchPaths = listOf("jvm")) {
        install(WebSockets) {
            pingPeriodMillis = 10000
        }
        install(ContentNegotiation) {
            json()
        }
        install(Sessions) {
            cookie<UserSessionData>(UserSessionData.COOKIE_NAME) {
                cookie.path = "/"
                cookie.httpOnly = false
                transform(SessionTransportTransformerMessageAuthentication(Env.sessionCookieSignKey))
            }
        }
        routing {
            get("/") { call.respondHtml(HttpStatusCode.OK, HTML::index) }

            authRoute()

            webSocket(Endpoints.websocketPrimary) {
                val user = call.sessions.get<UserSessionData>()
                    ?.let {
                        User.Instances[it.userId]
                    }
                    ?: run {
                        close(CloseReason(1000, "no session"))
                        return@webSocket
                    }
                println(">>>>> open [user: '${user.data.username}']")
                ClientConnection.run(websocketObject(), user)
                println(">>>>> close")
            }

            static("/static") { resources() }

            get("/*") { call.respondHtml(HttpStatusCode.NotFound, HTML::notFoundPage) }

        }
    }.start(wait = true)
}