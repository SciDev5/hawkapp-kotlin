package app

import UserDataFC
import csstype.em
import csstype.number
import data.channel.ChannelMessageData
import emotion.react.css
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import util.react.childElements

external interface FCChannelMessageProps : Props {
    var msg: ChannelMessageData
    var isUnsentFromSelf: Boolean
}

val FCChannelMessage = FC<FCChannelMessageProps> { props ->
    div {
        css {
            padding = 0.5.em
            opacity = if (props.isUnsentFromSelf)
                number(0.5)
            else
                null
        }
        div {
            UserDataFC {
                id = props.msg.sender
                withFound = childElements { (_, data) ->
                    +data.username
                }
                withLoading = childElements { _ ->
                    +"..."
                }
                withNotFound = childElements { id ->
                    +"[missing user #${
                        id.v.toULong().toString(16).padStart(16, '0')
                    }]"
                }
            }
        }
        div {
            +props.msg.content.t
        }
    }
}