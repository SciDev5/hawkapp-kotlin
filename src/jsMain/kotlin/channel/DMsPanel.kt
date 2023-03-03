package channel

import User
import data.TimestampedId
import data.channel.ChannelLookupData
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
import util.react.useTXR
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
        sendReceive<List<UserData>, TimestampedId.SerialBox>(
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

external interface DMsPanelProps : Props

val DMsPanel = FC<DMsPanelProps> { _ ->
    var zones by useState<Set<DMZoneData>>(emptySet())
    var selectedZone by useState<TimestampedId?>(null)

    val scope = useCoroutineScope()
    val txr = useTXR()

    useEffect(txr, scope) {
        val cleanupSignal = UntilLock().also { cleanup { it.unlock() } }

        scope.launch {
            watchDMList(txr, cleanupSignal).collect {
                zones = it
            }
        }
    }
    useEffect(zones.map { it.id }.toSet(), selectedZone) {
        var z = selectedZone
        if (z != null)
            if (zones.none { it.id == z })
                z = null
        z = z ?: zones.firstOrNull()?.id

        if (selectedZone != z)
            selectedZone = z
    }

    div {
        div { +"Your Messages:" }
        div {
            button {
                onClick = suspendCallback(scope) {
                    val username = window.prompt("username?:")
                        ?: return@suspendCallback
                    val user = User.Instances.withTxr(txr)
                        .lookupUsername(username)
                        ?: run {
                            window.alert("USER NOT FOUND")
                            return@suspendCallback
                        }
                    window.alert("making dm with '${user.username}'")

                    createDM(txr, setOf(user))
                }
                +"Begin Message"
            }
        }

        if (zones.isEmpty()) {
            div { +"lmao lonely" }
        }

        div {
            for (zone in zones) {
                div {
                    val isSelected = zone.id == selectedZone
                    if (isSelected)
                        +"> "
                    +":: ${zone.members.joinToString(", ") { it.id.toString() }}"
                    button {
                        onClick = suspendCallback(scope) {
                            if (window.confirm("u sure?"))
                                leaveDM(txr, zone)
                        }
                        +"x"
                    }
                    if (!isSelected)
                        button {
                            onClick = suspendCallback(scope) {
                                selectedZone = zone.id
                            }
                            +"select"
                        }
                }
            }
        }
        selectedZone?.also { zoneId ->
            div {
                MessageChannel {
                    channelLookup = ChannelLookupData.DM(zoneId)
                }
            }
        } ?: run {
            div {
                +"[[NO DM SELECTED]]"
            }
        }
    }
}