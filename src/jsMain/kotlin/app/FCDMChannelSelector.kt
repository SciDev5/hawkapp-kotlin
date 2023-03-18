package app

import UserCard
import csstype.Color
import data.TimestampedId
import data.channel.DMZoneData
import emotion.react.css
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
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


    div {
        onClick = suspendCallback(scope) {
            props.onSelect()
        }
        css {
            if (props.selected)
                background = Color("#f00")
            else
                hover {
                    background = Color("#7ff")
                }
        }
        if (isAnnouncements) div {
            if (canWrite) {
                + "Send Announcements"
            } else {
                + "Announcements"
            }
        }
        div {
            fun user(id: TimestampedId) {
                UserCard {
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
                    + " (alone)"
                }
            }
        }

        button {
            onClick = suspendCallback(scope) {
                props.onDelete()
            }
            + "leave"
        }
    }
}