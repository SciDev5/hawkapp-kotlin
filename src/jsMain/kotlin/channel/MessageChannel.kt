package channel

import app.FCChannelMessage
import app.FCChannelMessageInput
import csstype.*
import data.TimestampedId
import data.channel.ChannelData
import data.channel.ChannelLookupData
import data.channel.ChannelMessageData
import emotion.react.css
import kotlinx.coroutines.launch
import react.*
import react.dom.html.ReactHTML.div
import util.react.useCoroutineScope
import util.react.useTXR
import util.react.useUserId
import wsTransaction.WSTransactionBiChannel
import wsTransaction.runChannelUntilClose
import kotlin.random.Random

external interface MessageChannelProps : Props {
    var channelLookup: ChannelLookupData
}

val MessageChannel = FC<MessageChannelProps> { props ->
    val txr = useTXR()
    val scope = useCoroutineScope()
    val selfUserId = useUserId()

    val messages = useMemo { mutableListOf<ChannelMessageData>() }
    var messageUpdateId by useState(0)

    val sent = useMemo { mutableListOf<ChannelMessageData.Content>() }
    var sentBehindN by useState(0)

    var chan by useState<WSTransactionBiChannel<ChannelMessageData, ChannelMessageData.Content>?>(null)
    var failed by useState(false)
    var loading by useState(true)

    val sendMessage = useMemo<suspend (ChannelMessageData.Content) -> Unit>(sent, chan) {
        chan?.let {
            { msg ->
                sent.add(0, msg)
                it.send(msg)
            }
        } ?: { _ -> }
    }

    useEffect(
        txr,
        props.channelLookup::class,
        props.channelLookup.hashCode()
    ) {
        failed = false
        loading = true
        var ranCleanup = false
        val biChan = WSTransactionBiChannel<ChannelMessageData, ChannelMessageData.Content>()
        chan = biChan
        scope.launch {
            messages.clear()

            // TODO: fetch messages

            loading = false

            launch {
                txr.run(ChannelData.TransactionNames.CHANNEL_LISTEN) {
                    val ok = sendReceive<ChannelLookupData, Boolean>(props.channelLookup)
                    if (!ok) {
                        failed = true
                        return@run
                    }
                    runChannelUntilClose(biChan)
                }
            }
            launch {
                for (k in biChan.channelNumUnconfirmedSent) {
                    sentBehindN = k
                }
            }
            for (data in biChan) {
                if (!ranCleanup) {
                    messages.add(data)
                    messageUpdateId = Random.nextInt()
                }
            }
        }
        cleanup {
            ranCleanup = true
            scope.launch {
                biChan.close()
            }
        }
    }

    if (failed) div {
        +" !! FAILED !! "
    } else if (loading) div {
        + " ... loading ... "
    } else div {
        css {
            display = Display.flex
            flexDirection = FlexDirection.column
            height = 100.0.pct
        }
        div {
            css {
                display = Display.flex
                flexDirection = FlexDirection.columnReverse
                overflowY = Overflow.scroll
                overflowX = Overflow.clip

                flexGrow = number(1.0)
            }
            for (message in sent.slice(0 until sentBehindN)) {
                FCChannelMessage {
                    this.msg = ChannelMessageData(selfUserId, TimestampedId(0), message)
                    isUnsentFromSelf
                }
            }
            for (message in messages.reversed()) {
                FCChannelMessage {
                    this.msg = message
                }
            }
        }
        FCChannelMessageInput {
            this.sendMessage = sendMessage
        }
    }
}