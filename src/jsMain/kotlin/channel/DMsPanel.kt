package channel

import app.FCDMChannelSelector
import clientData.DMZone
import clientData.User
import csstype.*
import data.TimestampedId
import data.channel.ChannelLookupData
import data.channel.DMZoneData
import kotlinx.browser.window
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState
import style.*
import util.coroutine.UntilLock
import util.react.suspendCallback
import util.react.useCoroutineScope
import util.react.useTXR
import util.withTxr


external interface DMsPanelProps : Props

val DMsPanel = FC<DMsPanelProps> { _ ->
    var zones by useState<Set<DMZoneData>>(emptySet())
    var selectedZone by useState<TimestampedId?>(null)

    val scope = useCoroutineScope()
    val txr = useTXR()

    useEffect(txr, scope) {
        val cleanupSignal = UntilLock().also { cleanup { it.unlock() } }

        scope.launch {
            DMZone.Instances.watchList(cleanupSignal).collect {
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

    styledDiv("DMs", flexChild(), flexContainerHorizontal(), { height = 100.0.pct }) {
        styledDiv("list", flexChild(0.0), flexContainerVertical(), {
            width = 20.0.em
        }) {
            styledDiv("header", flexChild(0.0), flexContainerVertical()) {
                styled(button, "createDM", flexChild(0.0), InputStyle.base, {
                    margin = 0.5.em
                }) {
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

                        DMZone.Instances.withTxr(txr).create(setOf(user))
                    }
                    +"Begin Message"
                }
            }
            flexDividerHorizontal(0)
            styledDiv("dmsList", flexChild(), {
                overflowX = Overflow.hidden
            }) {
                if (zones.isEmpty()) {
                    styled(p, "noMessages", {
                        margin = 0.5.em
                    }) { +"No Message Channels" }
                }
                for (zone in zones) {
                    val isSelected = zone.id == selectedZone
                    FCDMChannelSelector {
                        this.zone = zone
                        this.selected = isSelected
                        this.onSelect = {
                            selectedZone = zone.id
                        }
                        this.onDelete = {
                            if (window.confirm("Are you sure you want to leave this message chain?"))
                                DMZone.Instances.withTxr(txr).leave(zone)
                        }
                    }
                }
            }
        }
        flexDividerVertical(0)
        styledDiv("messages", flexChild(), flexContainerHorizontal()) {
            selectedZone?.also { zoneId ->
                MessageChannel {
                    channelLookup = ChannelLookupData.DM(zoneId)
                }
            } ?: run {
                styledDiv("no selection", cssDMsMessagesLayout) {
                    styled(p, "message", cssTextCentered, { fontWeight = FontWeight.bold }) {
                        + ":: Select a Message Channel ::"
                    }
                }
            }
        }
    }
}