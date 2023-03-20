package me.scidev5.application

import Endpoints
import data.TimestampedId
import data.session.UserSessionData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
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


fun main() {
    databaseInit()

    val uK = User.Instances[TimestampedId(389384133)]!!

    embeddedServer(Netty, port = 8080, watchPaths = listOf("jvm")) {
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
            get("/r") {
                val n = (Math.random()*0x10000).toInt().toString(16).padStart(4,'0')
                uK.username = n
                call.respondText(n, ContentType.Text.Plain)
            }

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

            get("/") { call.respondHtml(HttpStatusCode.OK, HTML::index) }
            get("/*") { call.respondHtml(HttpStatusCode.OK, HTML::index) }
        }
    }.start(wait = true)
}