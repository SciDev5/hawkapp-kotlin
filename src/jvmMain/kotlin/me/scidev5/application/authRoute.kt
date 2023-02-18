package me.scidev5.application

import Endpoints
import data.session.HTTPLoginData
import data.session.UserSessionData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.authRoute() {
        post(Endpoints.Auth.login) {
            val loginRequest = call.receive<HTTPLoginData.ReqLogin>()
            val newSession = UserSessionData(loginRequest.username)
            call.sessions.set(newSession)
            call.respond<HTTPLoginData.ResLogin>(HttpStatusCode.OK, HTTPLoginData.ResLoginSuccess())
        }
        post(Endpoints.Auth.logout) {
            val had = call.sessions.get<UserSessionData>() != null
            call.sessions.clear(UserSessionData.COOKIE_NAME)
            call.respond(HttpStatusCode.OK, HTTPLoginData.ResLogout(wasChange = had))
        }
}