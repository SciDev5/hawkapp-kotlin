package me.scidev5.application

import me.scidev5.application.messageChannel.DMZone
import me.scidev5.application.messageChannel.MessageChannel
import me.scidev5.application.push.Notifications
import me.scidev5.application.ws.ServerWebsocket
import wsTransaction.KWSTransactor

class ClientConnection private constructor(
    ws: ServerWebsocket,
    val user: User,
) {
    private val wtx = KWSTransactor.build(ws) {
        handle(MessageChannel.Handle.listen(user))

        handle(DMZone.Handle.get(user))
        handle(DMZone.Handle.sync(user))
        handle(DMZone.Handle.watchOwnList(user))
        handle(DMZone.Handle.create(user))
        handle(DMZone.Handle.delete(user))

        handle(User.Handle.get())
        handle(User.Handle.sync())
        handle(User.Handle.lookupUsername())

        handle(Notifications.Handle.manage(user))
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