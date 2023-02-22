package me.scidev5.application.messageChannel

import data.TimestampedId
import data.channel.ChannelData
import data.channel.ChannelLookupData
import data.channel.ChannelMessageData
import kotlinx.coroutines.launch
import me.scidev5.application.ClientConnection
import me.scidev5.application.util.extensions.generate
import util.coroutine.MultiReceiverChannel
import wsTransaction.KWSTransactionHandle
import wsTransaction.WSTransactionBiChannel
import wsTransaction.runChannelUntilClose

class MessageChannel(data: ChannelData) {
    var data = data
        private set

    private val messages = mutableListOf<ChannelMessageData>()
    
    private val msgRelay = MultiReceiverChannel<ChannelMessageData>()

    private suspend fun send(sender: TimestampedId, content: ChannelMessageData.Content) {
        val msg = ChannelMessageData(
            sender,
            TimestampedId.generate(),
            content
        )
        
        messages.add(msg)
        msgRelay.send(msg)
        // TODO database things
    }

    suspend fun devive() {
        msgRelay.close()
    }

    companion object {
        private var TEMP_CHANNEL: MessageChannel? = null
        private suspend fun lookupChannel(
            lookupQuery: ChannelLookupData
        ): MessageChannel? {
            if (TEMP_CHANNEL == null)
                TEMP_CHANNEL = MessageChannel(ChannelData(name = "yeet", emptyArray()))
            return TEMP_CHANNEL
        }

        fun listenerHandle(
            connection: ClientConnection
        ) = KWSTransactionHandle(
                ChannelData.TransactionNames.CHANNEL_LISTEN
        ) {
                // Get channel identifier from client and search for it
                val channel = lookupChannel(nextData())
                    // Not found, send "false"
                    ?: run {
                        send(false)
                        return@KWSTransactionHandle
                    }
                // Has channel, continue
                send(true)

                val messageDataToSend = channel.msgRelay.Receiver()
                val toFromClientData = WSTransactionBiChannel<ChannelMessageData.Content,ChannelMessageData>()

                // Send messages from this channel to the client
                launch {
                    for (messageData in messageDataToSend)
                        toFromClientData.send(
                            messageData
                        )
                }
                // Relay messages from the client to the channel 
                launch {
                    for (content in toFromClientData)
                        channel.send(
                            sender = connection.user.id,
                            content
                        )
                }

                // Listen and close when done
                try {
                    runChannelUntilClose(toFromClientData)
                } finally {
                    messageDataToSend.closeReceiving()
                    toFromClientData.close()
                }
            }
    }
}