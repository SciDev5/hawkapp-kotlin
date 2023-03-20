import data.session.HTTPLoginData
import data.user.UserData
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.useState
import util.react.suspendCallback
import util.react.useCoroutineScope

val AppLogin = FC<Props> {
    val userId = Auth.useCurrentUserId()
    val scope = useCoroutineScope()

    var username by useState("")
    var password by useState("")

    var loginIssue by useState<HTTPLoginData.ResLoginFailure.Reason?>(null)
    var signupIssue by useState<HTTPLoginData.ResSignupFailure.Reason?>(null)
    val usernameValidityReport = UserData.Validate.username(username)
    val passwordValidityReport = UserData.Validate.password(password)

    val issueText = loginIssue?.name
        ?: signupIssue?.name
        ?: usernameValidityReport.failures.firstOrNull()?.let { "username - " + it::class.simpleName }
        ?: passwordValidityReport.failures.firstOrNull()?.let { "password - " + it::class.simpleName }

    if (issueText != null) {
        div {
            + issueText
        }
    }

    if (userId == null) {
        input {
            value = username
            onChange = { username = it.currentTarget.value }
            onBlur = { username = username.trim() }
            placeholder = "Username"
        }
        input {
            value = password
            onChange = { password = it.currentTarget.value }
            onBlur = { password = password.trim() }
            placeholder = "Password"
            type = InputType.password
        }
        button {
            onClick = suspendCallback(scope) {
                loginIssue = null
                signupIssue = null
                when (val res = Auth.login(username, password)) {
                    is HTTPLoginData.ResSuccess -> {
                        // do nothing, `Auth` will update hooks
                    }
                    is HTTPLoginData.ResLoginFailure -> {
                        loginIssue = res.reason
                    }
                }
            }
            +"login"
        }
        button {
            onClick = suspendCallback(scope) {
                loginIssue = null
                signupIssue = null

                when (val res = Auth.signup(UserData.Creation(username, password))) {
                    is HTTPLoginData.ResSuccess -> {
                        // do nothing, `Auth` will update hooks
                    }
                    is HTTPLoginData.ResSignupFailure -> {
                        signupIssue = res.reason
                    }
                }
            }
            +"signup"
        }
    } else {
        button {
            onClick = suspendCallback(scope) {
                Auth.logout()
            }
            +"logout"
        }
    }

    div {
        +"user id: ${userId?.v?.toString(16) ?: "<NULL>"}"
    }
}