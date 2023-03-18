package channel

import app.FCDMChannelSelector
import clientData.DMZone
import clientData.User
import csstype.*
import data.TimestampedId
import data.channel.ChannelLookupData
import data.channel.DMZoneData
import emotion.react.css
import kotlinx.browser.window
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

    div {
        div {
            +"Your Messages:"
        }
        div {
            css {
                position = Position.relative

                display = Display.flex
                flexDirection = FlexDirection.row

                height = 90.0.vh
                border = Border(1.0.px, LineStyle.solid)
                borderColor = Color("#000")
            }

            div {
                css {
                    flexGrow = number(0.0)
                    flexBasis = 10.0.em

                    height = 100.0.pct
                }
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

                            DMZone.Instances.withTxr(txr).create(setOf(user))
                        }
                        +"Begin Message"
                    }
                }
                if (zones.isEmpty()) {
                    div { +"lmao lonely" }
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
                            if (window.confirm("u sure?"))
                                DMZone.Instances.withTxr(txr).leave(zone)
                        }
                    }
                }
            }
            div {
                css {
                    height = 100.0.pct
                    flexGrow = number(1.0)
                }
                selectedZone?.also { zoneId ->
                    MessageChannel {
                        channelLookup = ChannelLookupData.DM(zoneId)
                    }
                } ?: run {
                    div {
                        +" -- Select a Message --"
                    }
                }
            }
        }
    }
}