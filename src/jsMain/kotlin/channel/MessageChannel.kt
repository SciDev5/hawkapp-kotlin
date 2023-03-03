package channel

import data.channel.ChannelData
import data.channel.ChannelLookupData
import data.channel.ChannelMessageData
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.useEffect
import react.useState
import util.react.suspendCallback
import util.react.useCoroutineScope
import util.react.useTXR
import wsTransaction.WSTransactionBiChannel
import wsTransaction.runChannelUntilClose

external interface MessageChannelProps : Props {
    var channelLookup: ChannelLookupData
}

val MessageChannel = FC<MessageChannelProps> { props ->
    val txr = useTXR()

    div {

        var txt by useState("-")
        var chan by useState<WSTransactionBiChannel<ChannelMessageData, ChannelMessageData.Content>?>(null)
        var behind by useState(0)
        var failed by useState(false)

        val scope = useCoroutineScope()
        useEffect(
            txr,
            props.channelLookup::class,
            props.channelLookup.hashCode()
        ) {
            var txt2 = "-"
            txt = txt2
            failed = false
            var ranCleanup = false
            val biChan = WSTransactionBiChannel<ChannelMessageData, ChannelMessageData.Content>()
            chan = biChan
            scope.launch {
                launch {
                    txr.run(ChannelData.TransactionNames.CHANNEL_LISTEN) {
                        val ok = sendReceive<ChannelLookupData,Boolean>(props.channelLookup)
                        if (!ok) {
                            failed = true
                            return@run
                        }
                        runChannelUntilClose(biChan)
                    }
                }
                launch {
                    for (k in biChan.channelNumUnconfirmedSent) {
                        behind = k
                    }
                }
                for (data in biChan) {
                    txt2 += "\n[${data.sender}:${data.content.t}]"
                    if (!ranCleanup) txt = txt2
                }
                txt2 += "<END>"
                if (!ranCleanup) txt = txt2
            }
            cleanup {
                ranCleanup = true
                scope.launch {
                    biChan.close()
                }
            }
        }
        var inpText by useState("")
        if (chan != null) {
            ReactHTML.div {
                ReactHTML.input {
                    value = inpText
                    onChange = {
                        inpText = it.currentTarget.value
                    }
                    onKeyDown = suspendCallback(scope) {
                        if (it.key == "Enter") {
                            chan!!.send(ChannelMessageData.Content(t = inpText))
                            inpText = ""
                        }
                    }
                }
            }
        }
        if (failed) +"[[FAILED]]"
        +"$txt ($behind)"
    }
}