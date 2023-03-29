package app

import csstype.em
import csstype.number
import data.channel.ChannelMessageData
import react.FC
import react.Props
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
import react.useState
import style.*
import util.react.suspendCallback
import util.react.useCoroutineScope

external interface FCChannelMessageInputProps : Props {
    var disabled: Boolean
    var sendMessage: suspend (msg: ChannelMessageData.Content) -> Unit
}

val FCChannelMessageInput = FC<FCChannelMessageInputProps> { props ->
    val scope = useCoroutineScope()

    var inputText by useState("")

    flexDividerHorizontal(0)
    styledDiv("send", flexChild(0.0), flexContainerHorizontal()) {
        if (props.disabled) {
            styled(span, "t0", {
                margin = 0.5.em
                opacity = number(0.75)

                fontSize = 0.75.em
                paddingBlock = 0.5.em
                paddingInline = 1.0.em
                borderRadius = 0.4.em
                background = StyleColors.bgLighten
            }, flexChild(0.0)) {
                +"This channel is read-only."
            }
        } else {
            styled(span, "t0", {
                margin = 0.5.em
                marginRight = 0.0.em
            }, flexChild(0.0)) {
                +"Send:"
            }
            styled(input, "textField", InputStyle.base, flexChild(), { margin = 0.5.em }) {
                value = inputText
                onChange = { inputText = it.currentTarget.value }
                onKeyDown = suspendCallback(scope) {
                    if (it.key == "Enter") {
                        props.sendMessage(
                            ChannelMessageData.Content(
                                t = inputText
                            )
                        )
                        inputText = ""
                    }
                }
            }
        }
    }
}