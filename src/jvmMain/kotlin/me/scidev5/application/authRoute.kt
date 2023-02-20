package me.scidev5.application

import Endpoints
import data.session.HTTPLoginData.*
import data.session.UserSessionData
import data.user.UserData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*


private fun PipelineContext<Unit, ApplicationCall>.setLoginSession(user: User) {
    call.sessions.set(UserSessionData(user.id))
}

private fun PipelineContext<Unit, ApplicationCall>.clearLoginSession(): Boolean {
    val wasLoggedIn = getLoginSession() != null
    call.sessions.clear(UserSessionData.COOKIE_NAME)
    return wasLoggedIn
}

fun PipelineContext<Unit, ApplicationCall>.getLoginSession() =
    call.sessions.get<UserSessionData>()

fun Route.authRoute() {
    post(Endpoints.Auth.login) {
        val loginRequest = call.receive<ReqLogin>().cleaned()

        val user = User.Instances.byUsername(loginRequest.username)

        val res: ResLogin = when {
            user == null -> ResLoginFailure(
                ResLoginFailure.Reason.NO_SUCH_USER
            )

            !user.checkPassword(loginRequest.password) ->
                ResLoginFailure(ResLoginFailure.Reason.BAD_CREDENTIALS)

            else -> {
                setLoginSession(user)
                ResSuccess(user.id)
            }
        }

        call.respond(HttpStatusCode.OK, res)
    }

    post(Endpoints.Auth.signup) {
        val (userData) = call.receive<ReqSignup>().cleaned()


        val usernameValidity = UserData.Validate.username(userData.username)
        val passwordValidity = UserData.Validate.password(userData.password)

        val res: ResSignup = when {
            User.Instances.byUsername(userData.username) != null -> ResSignupFailure(
                ResSignupFailure.Reason.USERNAME_TAKEN
            )

            usernameValidity.failed -> ResSignupFailure(
                ResSignupFailure.Reason.USERNAME_INVALID,
                *usernameValidity.failures.toTypedArray()
            )

            passwordValidity.failed -> ResSignupFailure(
                ResSignupFailure.Reason.PASSWORD_INVALID,
                *passwordValidity.failures.toTypedArray()
            )

            else -> {
                val user = User.Instances.create(userData)
                setLoginSession(user)
                ResSuccess(user.id)
            }
        }

        call.respond(HttpStatusCode.OK, res)
    }

    post(Endpoints.Auth.logout) {
        call.respond(HttpStatusCode.OK, ResLogout(wasChange = clearLoginSession()))
    }
}