package app

import UserNameText
import csstype.em
import csstype.number
import data.TimestampedId
import data.channel.DMZoneData
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import style.*
import util.react.suspendCallback
import util.react.useCoroutineScope
import util.react.useUserId

external interface FCDMChannelSelectorProps : Props {
    var selected: Boolean
    var onSelect: suspend () -> Unit
    var onDelete: suspend () -> Unit
    var zone: DMZoneData
}

val FCDMChannelSelector = FC<FCDMChannelSelectorProps> { props ->
    val selfUserId = useUserId()
    val scope = useCoroutineScope()

    val readOnlyMembers = props.zone.members.filterNot { it.writeAccess }
    val canWrite = readOnlyMembers.none { it.id == selfUserId }
    val isAnnouncements = readOnlyMembers.isNotEmpty()


    styled(div, "dmEntry", flexContainerHorizontal(), {
        background = if (props.selected)
            StyleColors.bgEm else StyleColors.transparent
        transition = StyleColors.bgFgTransition
        margin = 0.5.em
        borderRadius = 0.25.em
        if (!props.selected) hover {
            background = StyleColors.bgLightenHov
        }
    }) {
        onClick = suspendCallback(scope) {
            props.onSelect()
        }
        styledDiv("label", flexChild(), flexContainerVertical()) {
            if (isAnnouncements) styledDiv("announcementLabel", flexChild(0.0)) {
                if (canWrite) {
                    +"Send Announcements"
                } else {
                    +"Announcements"
                }
            }
            styledDiv("labelMain", flexChild(0.0), {
                padding = 0.5.em
            }) {
                fun user(id: TimestampedId) {
                    UserNameText {
                        this.id = id
                    }
                }
                if (isAnnouncements) {
                    val writers = props.zone.members
                        .filter { it.writeAccess }
                        .filterNot { it.id == selfUserId }
                    if (canWrite) {
                        // list readers
                        for (mem in readOnlyMembers) {
                            user(mem.id)
                        }
                    } else {
                        // list writers
                        for (mem in writers) {
                            user(mem.id)
                        }
                    }
                } else {
                    for (mem in props.zone.members.filterNot { it.id == selfUserId }) {
                        user(mem.id)
                    }
                    if (props.zone.members.size == 1) {
                        user(selfUserId)
                        styled(span, "aloneMarker", {
                            opacity = number(0.75)
                            marginInline = 0.5.em
                            fontSize = 0.75.em
                        }) {
                            +"(alone)"
                        }
                    }
                }
            }
        }

        styled(button, "leaveButton", flexChild(0.0), InputStyle.muted) {
            onClick = suspendCallback(scope) {
                props.onDelete()
            }
            + "x"
        }
    }
}