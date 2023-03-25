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
import me.scidev5.application.push.Notifications
import me.scidev5.application.util.Env
import me.scidev5.application.ws.websocketObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.Charset
import java.security.Security

fun HTML.index() {
    head {
        title("HawkApp")
        link(rel = "icon", type = "image/x-icon", href = "/static/favicon.ico")
        link(rel = "manifest", href = "/manifest.json")

        link(rel = "preconnect", href = "https://fonts.googleapis.com")
        link(rel = "preconnect", href = "https://fonts.gstatic.com")
        link(
            href = "https://fonts.googleapis.com/css2?family=Raleway:wght@200;400;700&display=swap",
            rel = "stylesheet"
        )
    }
    body {
        div {
            id = "root"
        }
        script(src = Endpoints.jsMain) {}
        noScript {
            +"This app requires JavaScript, enable it or it'll keep not working"
        }
    }
}

private class ResourceFetchingDummyClass

fun main() {
    Security.addProvider(BouncyCastleProvider())
    databaseInit()

    val uK = User.Instances[TimestampedId(389384133)]!!

    val swCode = ResourceFetchingDummyClass::class.java.classLoader.getResource("sw.js")?.readBytes()
        ?: """console.error("SW failed on server: resource not found");""".trimIndent()
            .toByteArray(Charset.defaultCharset())
    val manifestJson = ResourceFetchingDummyClass::class.java.classLoader.getResource("manifest.json")?.readBytes()
        ?: "{}".toByteArray(Charset.defaultCharset())

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
                val n = (Math.random() * 0x10000).toInt().toString(16).padStart(4, '0')
                uK.username = n
                call.respondText(n, ContentType.Text.Plain)
            }
            get("/test") {
                val user = call.sessions.get<UserSessionData>()
                    ?.let {
                        User.Instances[it.userId]
                    }
                    ?: run {
                        call.respondText("no login", ContentType.Text.Plain, HttpStatusCode.Forbidden)
                        return@get
                    }
                Notifications.test(user)
                call.respondText("sent", ContentType.Text.Plain, HttpStatusCode.OK)
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

            get(Endpoints.swMain) {
                call.respondBytes(
                    swCode,
                    ContentType.Application.JavaScript
                )
            }
            get("/manifest.json") {
                call.respondBytes(
                    manifestJson,
                    ContentType.Application.JavaScript
                )
            }

            static("/static") { resources() }

            get("/") { call.respondHtml(HttpStatusCode.OK, HTML::index) }
            get("/*") { call.respondHtml(HttpStatusCode.OK, HTML::index) }
        }
    }.start(wait = true)
}