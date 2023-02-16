package me.scidev5.application

import Endpoints
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
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
        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            webSocket(Endpoints.websocketPrimary) {
                println(">>>>> open")
                val ws = websocketObject()
                ClientConnection(ws)
                launch {
                    ws.waitOpenOrClose()
                    println(">>> open or close")
                    ws.waitClosed()
                    println(">>> close")
                }
                ws.sendOutgoingFrames()
                println(">>>>> closed")
            }
            static("/static") {
                resources()
            }
        }
    }.start(wait = true)
}