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
import util.react.useCoroutineScope
import wsTransaction.KWSTransactor
import wsTransaction.WSTransactionBiChannel
import wsTransaction.runChannelUntilClose

external interface MessageChannelProps : Props {
    var txr: KWSTransactor
    var channelLookup: ChannelLookupData
}

val MessageChannel = FC<MessageChannelProps> { props ->

    div {

        var txt by useState("-")
        var chan by useState<WSTransactionBiChannel<ChannelMessageData, ChannelMessageData.Content>?>(null)
        var behind by useState(0)
        var failed by useState(false)

        val txr = props.txr
        val scope = useCoroutineScope()
        useEffect(txr,props.channelLookup) {
            failed = false
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
                var txt2 = txt
                for (data in biChan) {
                    txt2 += "\n[${data.sender}:${data.content.t}]"
                    txt = txt2
                }
                txt2 += "<END>"
                txt = txt2
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
                }
                ReactHTML.button {
                    onClick = {
                        scope.launch {
                            chan!!.send(ChannelMessageData.Content(t = inpText))
                        }
                    }
                    +"send"
                }
            }
        }
        if (failed) +"[[FAILED]]"
        +"$txt ($behind)"
    }
}