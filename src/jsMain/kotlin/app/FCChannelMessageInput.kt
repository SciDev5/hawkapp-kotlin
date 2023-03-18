package app

import csstype.number
import data.channel.ChannelMessageData
import emotion.react.css
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.useState
import util.react.suspendCallback
import util.react.useCoroutineScope

external interface FCChannelMessageInputProps : Props {
    var sendMessage: suspend (msg: ChannelMessageData.Content) -> Unit
}

val FCChannelMessageInput =  FC<FCChannelMessageInputProps>{ props ->
    val scope = useCoroutineScope()

    var inputText by useState("")

    div {
        css {
            flexGrow = number(0.0)
        }
        input {
            value = inputText
            onChange = { inputText = it.currentTarget.value }
            onKeyDown = suspendCallback(scope) {
                if (it.key == "Enter") {
                    props.sendMessage(ChannelMessageData.Content(
                        t = inputText
                    ))
                    inputText = ""
                }
            }
        }
    }
}