import clientData.User
import data.TimestampedId
import data.user.UserData
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.useEffect
import react.useState
import util.coroutine.UntilLock
import util.react.ComposedElements
import util.react.childElements
import util.react.useCoroutineScope
import util.react.useTXR
import util.withTxr

external interface UserFCProps : Props {
    var id: TimestampedId
    var withFound: ComposedElements<Pair<User,UserData>>
    var withNotFound: ComposedElements<TimestampedId>
    var withLoading: ComposedElements<TimestampedId>
}
val UserDataFC = FC<UserFCProps> { props ->
    var user by useState<User?>(null)
    var loaded by useState(false)
    var userWithData by useState<Pair<User,UserData>?>(null)
    val scope = useCoroutineScope()

    val txr = useTXR()

    useEffect(
        props.id,
        txr
    ) {
        val untilCleaned = UntilLock()
        scope.launch {
            val userFound = User.Instances.withTxr(txr).get(props.id)

            if (!untilCleaned.locked) return@launch

            loaded = true
            user = userFound
            userWithData = userFound?.let { Pair(it, it.data) }

            if (userFound != null) {
                for (userDataUpdate in userFound.watch(untilCleaned))
                    userWithData = Pair(userFound, userDataUpdate)
            }
        }
        cleanup {
            untilCleaned.unlock()
            loaded = false
        }
    }

    if (loaded) {
        userWithData?.also {
            props.withFound(this, it)
        } ?: run {
            props.withNotFound(this, props.id)
        }
    } else {
        props.withLoading(this, props.id)
    }
}

external interface UserDataReceiverProps : Props {
    var id: TimestampedId
}
val UserCard = FC<UserDataReceiverProps> { props ->
    UserDataFC {
        this.id = props.id
        withNotFound = childElements { id ->
            + "<@$id>"
        }
        withLoading = childElements { id ->
            + "...<@$id>"
        }
        withFound = childElements { (_,userData) ->
            + "'${userData.username}'"
        }
    }
}