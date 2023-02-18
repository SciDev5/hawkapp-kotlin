package me.scidev5.application

import Endpoints
import TestMessage
import data.session.LoginRequestData
import data.session.UserSessionData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.html.*
import me.scidev5.application.ws.websocketObject

fun HTML.index() {
    head {
        title("HawkApp")
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
    embeddedServer(Netty, port = 8080, host = "127.0.0.1", watchPaths = listOf("jvm")) {
        install(WebSockets) {
            pingPeriodMillis = 10000
        }
        install(ContentNegotiation) {
            json()
        }
        install(Sessions) {
            cookie<UserSessionData>(UserSessionData.COOKIE_NAME) {

            }
        }
        routing {
            get("/test") {
                call.respond(TestMessage("helo world"))
            }
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            route("/auth") {
                post("/login") {
                    val loginRequest = call.receive<LoginRequestData>()
                    val newSession = UserSessionData(loginRequest.nickname)
                    call.sessions.set(newSession)
                    call.respond(HttpStatusCode.OK, newSession)
                }
                post("/logout") {
                    call.sessions.clear(UserSessionData.COOKIE_NAME)
                    call.respond(HttpStatusCode.OK, true)
                }
            }
            get("/*") {
               call.respondText("404 lmao", ContentType.Text.Plain, HttpStatusCode.NotFound)
            }
            webSocket(Endpoints.websocketPrimary) {
                val session = call.sessions.get<UserSessionData>()
                if (session == null) {
                    close(CloseReason(1000, "no session"))
                    return@webSocket
                }
                println(">>>>> open")
                ClientConnection.run(websocketObject(), session)
                println(">>>>> close")
            }
            static("/static") {
                resources()
            }
        }
    }.start(wait = true)
}