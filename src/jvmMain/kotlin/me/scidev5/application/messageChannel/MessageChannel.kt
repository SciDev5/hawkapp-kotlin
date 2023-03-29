package me.scidev5.application.messageChannel

import data.TimestampedId
import data.channel.ChannelData
import data.channel.ChannelLookupData
import data.channel.ChannelMessageData
import kotlinx.coroutines.launch
import me.scidev5.application.User
import me.scidev5.application.util.extensions.generate
import util.coroutine.MultiReceiverChannel
import wsTransaction.KWSTransactionHandle
import wsTransaction.WSTransactionBiChannel
import wsTransaction.runChannelUntilClose

class MessageChannel(
    val permissions: ChannelPermissionSet,
    val distributeMessage: suspend (msg: ChannelMessageData) -> Unit
) {
    private val viewingChannelMutable = mutableSetOf<User>()
    val usersViewingChannel:Set<User> get() = viewingChannelMutable

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
        distributeMessage(msg)
    }

    var onClose:(()->Unit)? = null
    fun close() {
        msgRelay.close()
        onClose?.invoke()
    }
    fun closeIfNoViewers() {
        if (msgRelay.closeIfNoReceiversLeft()) {
            onClose?.invoke()
        }
    }

    data class Permissions(
        val join: Boolean,
        val write: Boolean
    )

    object Handle {
        private fun lookupChannel(
            lookupQuery: ChannelLookupData,
            user: User
        ): MessageChannel? = when (lookupQuery) {
            is ChannelLookupData.DM ->
                DMZone.Instances[lookupQuery.id]?.channel
            else -> null
        }?.let {
            if (it.permissions[user].use)
                it
            else
                null
        }

        fun listen(
            user: User
        ) = KWSTransactionHandle(
            ChannelData.TransactionNames.CHANNEL_LISTEN
        ) {
            // Get channel identifier from client and search for it
            val channel = lookupChannel(nextData(), user)
            // Not found, send "false"
                ?: run {
                    send(false)
                    return@KWSTransactionHandle
                }
            // Has channel, continue
            send(true)

            nextEmpty()
            send(channel.permissions[user].write)

            nextEmpty()
            send(channel.messages)

            val messageDataToSend = channel.msgRelay.Receiver()
            val toFromClientData = WSTransactionBiChannel<ChannelMessageData.Content, ChannelMessageData>()

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
                        sender = user.id,
                        content
                    )
            }

            // Listen and close when done
            try {
                channel.viewingChannelMutable.add(user)
                runChannelUntilClose(toFromClientData)
            } finally {
                channel.viewingChannelMutable.remove(user)
                messageDataToSend.closeReceiving()
                toFromClientData.close()

                channel.closeIfNoViewers()
            }
        }
    }
}