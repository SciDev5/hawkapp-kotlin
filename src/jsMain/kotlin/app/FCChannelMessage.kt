package app

import UserNameText
import csstype.em
import csstype.number
import data.channel.ChannelMessageData
import react.FC
import react.Props
import react.dom.html.ReactHTML.span
import style.StyleColors
import style.flexChild
import style.styled
import style.styledDiv
import util.react.useUserId
import kotlin.js.Date

external interface FCChannelMessageProps : Props {
    var msg: ChannelMessageData
    var isUnsentFromSelf: Boolean
}

val FCChannelMessage = FC<FCChannelMessageProps> { props ->
    val selfId = useUserId()
    styledDiv("message", flexChild(0.0), {
        padding = 0.5.em
        opacity = if (props.isUnsentFromSelf) number(0.5) else null
    }) {
        styledDiv("header", {
            fontSize = 0.8.em
            background = if (props.msg.sender == selfId)
                StyleColors.bgSlightEm else StyleColors.bgLighten
            padding = 0.25.em
        }) {
            UserNameText {
                id = props.msg.sender
            }
            styled(span, "time", {
                opacity = number(0.7)
                marginInline = 0.5.em
            }) {
                + Date(props.msg.msgId.timestampPart * 1000).toLocaleString()
            }
        }
        styledDiv("content", { marginTop = 0.25.em }) {
            +props.msg.content.t
        }
    }
}