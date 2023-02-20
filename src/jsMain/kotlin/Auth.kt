import data.TimestampedId
import data.session.HTTPLoginData.*
import data.session.UserSessionData
import data.user.UserData
import kotlinx.browser.document
import org.w3c.dom.url.URLSearchParams
import react.useEffect
import react.useMemo
import react.useState
import util.Fetch
import util.extensions.decodeURIEntries
import util.extensions.entries

object Auth {
    private var userId = URLSearchParams(document.cookie)
        .entries[UserSessionData.COOKIE_NAME]
        ?.let {
            it.split("/")[0]
        }?.let {
            URLSearchParams(it).entries[UserSessionData::userId.name]
                ?.substring(2)
        }?.let {
            decodeURIEntries(it).entries[TimestampedId::v.name]
                ?.substring(2)
                ?.toLong()
        }?.let {
            TimestampedId(it)
        }

    suspend fun login(username: String, password: String) =
        Fetch.post<ReqLogin, ResLogin>(
            Endpoints.Auth.login,
            ReqLogin(
                username, password
            )
        ).also {
            if (it is ResSuccess)
                updateSession(it.userId)
        }

    suspend fun signup(data: UserData.Creation) =
        Fetch.post<ReqSignup, ResSignup>(
            Endpoints.Auth.signup,
            ReqSignup(data)
        ).also {
            if (it is ResSuccess)
                updateSession(it.userId)
        }

    suspend fun logout() =
        Fetch.post<ResLogout>(
            Endpoints.Auth.logout
        ).also {
            updateSession(null)
        }


    private val sessionUpdateHandlers = mutableSetOf<(TimestampedId?) -> Unit>()
    fun useCurrentUserId(): TimestampedId? {
        var userIdState by useState(userId)
        val setState = useMemo(userIdState) {
            { newUserId: TimestampedId? -> userIdState = newUserId }
        }
        useEffect(userIdState) {
            sessionUpdateHandlers.add(setState)
            cleanup {
                sessionUpdateHandlers.remove(setState)
            }
        }
        return userIdState
    }

    private fun updateSession(id: TimestampedId?) {
        userId = id
        sessionUpdateHandlers.forEach { it(id) }
    }
}