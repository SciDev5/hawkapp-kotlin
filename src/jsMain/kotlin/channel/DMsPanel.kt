package channel

import User
import data.TimestampedId
import data.channel.DMZoneData
import data.user.UserData
import kotlinx.browser.window
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.useEffect
import react.useState
import util.coroutine.UntilLock
import util.react.suspendCallback
import util.react.useCoroutineScope
import wsTransaction.KWSTransactor
import wsTransaction.syncReceive

fun watchDMList(
    txr: KWSTransactor,
    closeEarlySignal: UntilLock
) = flow {
    txr.run(DMZoneData.TransactionNames.WATCH_LIST) {
        syncReceive<Set<DMZoneData>>(closeEarlySignal) {
            emit(it)
        }
    }
}
suspend fun createDM(
    txr: KWSTransactor,
    writers: Set<User>
) =
    txr.run(DMZoneData.TransactionNames.CREATE) {
        sendReceive<List<UserData>,TimestampedId.SerialBox>(
            writers.map { it.data }
        ).v
    }
suspend fun leaveDM(
    txr: KWSTransactor,
    zone: DMZoneData
) =
    txr.run(DMZoneData.TransactionNames.LEAVE) {
        send(zone.id.serial())
    }

external interface DMsPanelProps : Props {
    var txr: KWSTransactor
}

val DMsPanel = FC<DMsPanelProps> { props ->
    var zones by useState<Set<DMZoneData>>(emptySet())

    val scope = useCoroutineScope()

    useEffect(props.txr, scope) {
        val cleanupSignal = UntilLock().also { cleanup { it.unlock() } }

        scope.launch {
            watchDMList(props.txr, cleanupSignal).collect {
                zones = it
            }
        }
    }

    div {
        div { +"Your Messages:" }
        div {
            button {
                onClick = suspendCallback(scope) {
                    val username = window.prompt("username?:")
                        ?: return@suspendCallback
                    val user = User.Instances.withTxr(props.txr)
                        .lookupUsername(username)
                        ?: run {
                            window.alert("USER NOT FOUND")
                            return@suspendCallback
                        }
                    window.alert("making dm with '${user.username}'")

                    createDM(props.txr, setOf(user))
                }
                + "Begin Message"
            }
        }

        if (zones.isEmpty()) {
            div { +"lmao lonely" }
        }

        div {
            for (zone in zones) {
                div {
                    + ">> ${zone.members.joinToString(" & ") { it.id.randomPart.toString(36) }}"
                    button {
                        onClick = suspendCallback(scope) {
                            if (window.confirm("u sure?"))
                                leaveDM(props.txr, zone)
                        }
                        + "x"
                    }
                }
            }
        }
    }
}