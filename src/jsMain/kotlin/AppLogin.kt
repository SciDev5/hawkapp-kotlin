import csstype.FontWeight
import csstype.TextAlign
import csstype.em
import data.session.HTTPLoginData
import data.user.UserData
import kotlinx.browser.window
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
import react.router.dom.Link
import react.router.useNavigate
import react.useState
import style.*
import util.react.suspendCallback
import util.react.useCoroutineScope

val AppLogin = FC<Props> {
    val userId = Auth.useCurrentUserId()
    val scope = useCoroutineScope()
    val navigate = useNavigate()

    var username by useState("")
    var password by useState("")

    var loginIssue by useState<HTTPLoginData.ResLoginFailure.Reason?>(null)
    var signupIssue by useState<HTTPLoginData.ResSignupFailure.Reason?>(null)
    val usernameValidityReport = UserData.Validate.username(username)
    val passwordValidityReport = UserData.Validate.password(password)

    fun clearIssues() {
        loginIssue = null
        signupIssue = null
    }

    val filled = username.isNotEmpty() && password.isNotEmpty()
    val issue =
        loginIssue?.name
            ?: signupIssue?.name
            ?: usernameValidityReport.failures.firstOrNull()?.let { "username - " + it::class.simpleName }
            ?: passwordValidityReport.failures.firstOrNull()?.let { "password - " + it::class.simpleName }
    val issueAffectsLogin =
        loginIssue != null || usernameValidityReport.failed || passwordValidityReport.failed
    val issueAffectsSignup =
        signupIssue != null || usernameValidityReport.failed || passwordValidityReport.failed

    centeredPaneFlex("_", 25.0.em) {
        val rowCss: CSSCallback = {
            flexChild(0.0, 0.0)()
            marginTop = 1.0.em
        }

        styledDiv("uid", flexChild(0.0,0.0), {
            marginTop = 2.0.em
            marginBottom = 1.0.em
            textAlign = TextAlign.center
        }) {
            +"User Id: "
            styled(span,"0", { fontWeight = FontWeight.bold }) {
                +(userId?.v?.toString(16) ?: "<NULL>")
            }
        }

        flexDividerHorizontal(0, 0.1.em)

        if (userId == null) {
            styled(h1,"hdr", {
                textAlign = TextAlign.center
            }) {
                + "Login / Create"
            }
            styled(input, "u", rowCss, InputStyle.lined) {
                value = username
                onChange = { username = it.currentTarget.value; clearIssues() }
                onBlur = { username = username.trim() }
                placeholder = "Username"
            }
            styled(input, "p", rowCss, InputStyle.lined) {
                value = password
                onChange = { password = it.currentTarget.value; clearIssues() }
                onBlur = { password = password.trim() }
                placeholder = "Password"
                type = InputType.password
            }
            if (issue != null && filled) {
                styledDiv("e-l", {
                    background = StyleColors.warnBg
                    paddingBlock = 0.3.em
                    paddingInline = 0.5.em
                    fontSize = 0.9.em
                }, rowCss) {
                    +issue
                }
            }

            styledDiv("b", rowCss, flexContainerHorizontal()) {
                styled(button, "l", flexChild(), InputStyle.emphatic) {
                    disabled = issueAffectsLogin

                    onClick = suspendCallback(scope) {
                        when (val res = Auth.login(username, password)) {
                            is HTTPLoginData.ResSuccess -> {
                                // do nothing, `Auth` will update hooks
                                navigate(Endpoints.Page.main)
                            }

                            is HTTPLoginData.ResLoginFailure -> {
                                loginIssue = res.reason
                            }
                        }
                    }
                    +"Log In"
                }
                styledDiv("b0", flexChild(0.0, 0.0), { width = 1.0.em }) { }
                styled(button, "s", flexChild(), InputStyle.lined) {
                    disabled = issueAffectsSignup
                    onClick = suspendCallback(scope) {
                        when (val res = Auth.signup(UserData.Creation(username, password))) {
                            is HTTPLoginData.ResSuccess -> {
                                // do nothing, `Auth` will update hooks
                                navigate(Endpoints.Page.main)
                            }

                            is HTTPLoginData.ResSignupFailure -> {
                                signupIssue = res.reason
                            }
                        }
                    }
                    +"Create Account"
                }
            }
            styled(button, "TBP", rowCss, InputStyle.base) {
                onClick = suspendCallback(scope) {
                    when (val res = Auth.login(window.prompt("uname: ") ?: "", "")) {
                        is HTTPLoginData.ResSuccess -> {
                            // do nothing, `Auth` will update hooks
                        }

                        is HTTPLoginData.ResLoginFailure -> {
                            loginIssue = res.reason
                        }
                    }
                }
                +"<DEBUG LOGIN>"
            }
        } else {
            styled(h1,"hdr", {
                textAlign = TextAlign.center
            }) {
                + "Account"
            }
            styled(button, "o", rowCss, InputStyle.lined) {
                onClick = suspendCallback(scope) {
                    Auth.logout()
                }
                +"Log Out"
            }
            styled(Link, "lr", rowCss, InputStyle.emphatic) {
                to = Endpoints.Page.main
                +"Go To Main Page"
            }
        }
    }
}