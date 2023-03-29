package channel

import app.FCChannelMessage
import app.FCChannelMessageInput
import csstype.*
import data.TimestampedId
import data.channel.ChannelData
import data.channel.ChannelLookupData
import data.channel.ChannelMessageData
import kotlinx.coroutines.launch
import react.*
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import style.*
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

    var isReadonly by useState(true)

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
        isReadonly = true
        var ranCleanup = false
        val biChan = WSTransactionBiChannel<ChannelMessageData, ChannelMessageData.Content>()
        chan = biChan
        scope.launch {
            messages.clear()

            launch {
                txr.run(ChannelData.TransactionNames.CHANNEL_LISTEN) {
                    val ok = sendReceive<ChannelLookupData, Boolean>(props.channelLookup)
                    loading = false
                    if (!ok) {
                        failed = true
                        return@run
                    }
                    sendEmpty()
                    val canWrite = nextData<Boolean>()
                    isReadonly = !canWrite

                    sendEmpty()
                    messages.addAll(nextData<List<ChannelMessageData>>())
                    messages.sortBy { it.msgId.timestampPart } // sort oldest to most recent
                    messageUpdateId = Random.nextInt()

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

    if (failed) styledDiv("failed", cssDMsMessagesLayout) {
        +" !! FAILED !! "
    } else if (loading) styledDiv("loading", cssDMsMessagesLayout) {
        +" ... loading ... "
    } else styledDiv("msgBox", cssDMsMessagesLayout, {
        overflowY = Overflow.hidden
    }, flexContainerVertical()) {
        styledDiv("receive", flexChild(), flexContainer(FlexDirection.columnReverse), {
            overflowY = Overflow.scroll
            overflowX = Overflow.clip
        }) {
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
            styledDiv("beginning", flexContainerVertical(), {
                margin = 1.0.em
            }, cssTextCentered) {
                styled(h1, "hdr", flexChild(0.0)) {
                    +":: beginning of channel ::"
                }
                styled(p, "info", flexChild(0.0), {
                    opacity = number(0.5)
                    fontSize = 0.5.em
                }) {
                    +"[ CH : ${props.channelLookup} ]"
                }

                flexDividerHorizontal(0)
            }
        }
        FCChannelMessageInput {
            this.disabled = isReadonly
            this.sendMessage = sendMessage
        }
    }
}