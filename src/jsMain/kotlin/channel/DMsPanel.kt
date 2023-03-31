package channel

import app.FCDMChannelSelector
import clientData.DMZone
import clientData.User
import csstype.*
import data.TimestampedId
import data.channel.ChannelLookupData
import data.channel.DMZoneData
import kotlinx.browser.window
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.router.dom.useSearchParams
import react.useEffect
import react.useState
import serviceWorker.Notifications
import style.*
import util.coroutine.UntilLock
import util.react.*
import util.withTxr


external interface DMsPanelProps : Props

val DMsPanel = FC<DMsPanelProps> { _ ->
    val (params, setParams) = useSearchParams()

    var zones by useState<Set<DMZoneData>>(emptySet())
    var selectedZone by useState<TimestampedId?>(null)

    val scope = useCoroutineScope()
    val txr = useTXR()

    var notificationState by useState(Notifications.permission)

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
        val locId = params.get("ch")?.toLongOrNull(16)?.let { TimestampedId(it) }
        z = z ?: (zones.firstOrNull { it.id == locId } ?: zones.firstOrNull())?.id

        if (selectedZone != z)
            selectedZone = z

        if (locId != selectedZone && selectedZone != null) {
            params.set("ch", selectedZone!!.v.toString(16))
            setParams(params)
        }
    }

    val requestRecipientUsernameModal = useQuestionModal("Enter recipient username:")
    val requestAnnouncementRecipientsModal = UseModal {
        ResolutionModal<Set<User>?>(defaultRes = { null }) { res, escape ->
            var usernameList by useState(listOf<String>())

            for (i in usernameList.indices) {
                styledDiv("user$i", flexContainerHorizontal(), {
                    marginBlock = 0.2.em
                }) {
                    styled(button, "x", flexChild(0.0), InputStyle.base) {
                        onClick = {
                            usernameList = usernameList.subList(0, i) + usernameList.subList(i + 1, usernameList.size)
                        }
                        +"✕"
                    }
                    styled(input, "i", InputStyle.lined, flexChild()) {
                        value = usernameList[i]
                        onChange = {
                            val v = it.currentTarget.value
                            usernameList =
                                usernameList.subList(0, i) + listOf(v) + usernameList.subList(i + 1, usernameList.size)
                        }
                    }
                    styledDiv("", {
                        marginInline = 0.5.em
                        marginBlock = 0.4.em
                        width = 8.0.em
                        fontSize = 0.75.em
                        textAlign = TextAlign.center
                    }, flexChild(0.0)) {
                        var found: Boolean? by useState(null)
                        useEffect(usernameList[i]) {
                            found = null
                            scope.launch {
                                val user = User.Instances.lookupUsername(usernameList[i])
                                found = user != null
                                user?.also { User.Instances.forget(it) }
                            }.also {
                                cleanup { it.cancel() }
                            }
                        }
                        when (found) {
                            true -> {}
                            false -> styled(span, "err", {
                                background = StyleColors.warnBg
                                marginInline = 0.5.em
                            }) { +"user not found" }

                            null -> +"..."
                        }
                    }
                }
            }
            styledDiv("+", flexChild(0.0), flexContainerHorizontal()) {
                styled(button, "+", flexChild(), InputStyle.base) {
                    +"Add user"
                    onClick = {
                        usernameList = usernameList + listOf("")
                    }
                }
            }
            flexDividerHorizontal(0, 0.5.em)
            styledDiv("buttons", flexChild(), flexContainerHorizontal()) {
                styled(button, "ok", InputStyle.emphatic, flexChild()) {
                    +"ok"
                    onClick = suspendCallback(scope) {
                        val userChannel = Channel<User>(Channel.UNLIMITED)
                        coroutineScope {
                            User.Instances.withTxr(txr)
                            for (username in usernameList) {
                                launch {
                                    val user = User.Instances.lookupUsername(username)
                                    user?.let {
                                        userChannel.send(it)
                                    }
                                }
                            }
                        }
                        userChannel.close()
                        val users = mutableSetOf<User>()
                        for (user in userChannel) {
                            users.add(user)
                        }
                        res(users)
                    }
                }
                styled(button, "cancel", InputStyle.lined, flexChild()) {
                    +"cancel"
                    onClick = { escape() }
                }
            }
        }
    }

    styledDiv("DMs", flexChild(), flexContainerHorizontal(), { height = 100.0.pct }) {
        styledDiv("list", flexChild(0.0), flexContainerVertical(), {
            width = 20.0.em
        }) {
            styledDiv("header", flexChild(0.0), flexContainerVertical()) {
                if (notificationState == Notifications.Permission.DEFAULT) {
                    styled(button, "subNotify", flexChild(0.0), InputStyle.emphatic, {
                        margin = 0.5.em
                    }) {
                        onClick = suspendCallback(scope) {
                            Notifications.unsubscribe(txr)
                            Notifications.requestPermissionAndSubscribe(txr)
                            notificationState = Notifications.permission
                        }
                        +"Activate Notifications"
                    }
                }
                styled(button, "createAn", flexChild(0.0), InputStyle.base, {
                    margin = 0.5.em
                }) {
                    onClick = suspendCallback(scope) {
                        val users = requestAnnouncementRecipientsModal.request()
                            ?: return@suspendCallback

                        window.alert(
                            "making announcement with with:\n${
                                users.joinToString("\n") {
                                    ":: '${it.username}'"
                                }
                            }"
                        )

                        DMZone.Instances.withTxr(txr).create(emptySet(), users)
                    }
                    +"Begin Announcements"
                }
                styled(button, "createDM", flexChild(0.0), InputStyle.base, {
                    margin = 0.5.em
                }) {
                    onClick = suspendCallback(scope) {
                        val username = requestRecipientUsernameModal.request()
                            ?: return@suspendCallback
                        val user = User.Instances.withTxr(txr)
                            .lookupUsername(username)
                            ?: run {
                                window.alert("USER NOT FOUND")
                                return@suspendCallback
                            }
                        window.alert("making message channel with '${user.username}'")

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
                        +":: Select a Message Channel ::"
                    }
                }
            }
        }
    }
}