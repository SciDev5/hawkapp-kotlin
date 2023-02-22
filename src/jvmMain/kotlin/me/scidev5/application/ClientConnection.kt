package me.scidev5.application

import me.scidev5.application.messageChannel.MessageChannel
import me.scidev5.application.ws.ServerWebsocket
import wsTransaction.KWSTransactor

class ClientConnection private constructor(
    ws: ServerWebsocket,
    val user: User,
) {
    val wtx = KWSTransactor.build(ws) {
        handle(MessageChannel.listenerHandle(this@ClientConnection))
    }


    companion object {
        suspend fun run(
            ws: ServerWebsocket,
            user: User
        ) {
            ClientConnection(ws, user)
            ws.sendOutgoingFrames()
        }
    }
}