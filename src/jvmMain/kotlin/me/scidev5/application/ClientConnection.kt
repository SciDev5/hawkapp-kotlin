package me.scidev5.application

import wsTransaction.KWSTransactor
import util.coroutine.MultiReceiverChannel
import TestMessage
import wsTransaction.WSTransactionBiChannel
import kotlinx.coroutines.*
import me.scidev5.application.ws.ServerWebsocket
import wsTransaction.runChannelUntilClose

class ClientConnection(
    ws: ServerWebsocket,
) {
    val wtx = KWSTransactor.build(ws) {
        handle("h") {
            val msgReceiver = msgs.Receiver()
            val biChan = WSTransactionBiChannel<TestMessage, TestMessage>()
            try {
                launch {
                    for (frame in msgReceiver)
                        biChan.send(frame)
                }
                launch {
                    delay(1000)
                    biChan.send(TestMessage("helo"))
                }
                launch {
                    for (frame in biChan) {
                        println("HAS FRAME: $frame")
                        msgs.send(frame)
                        println("SENT")
                    }
                }
                runChannelUntilClose(biChan)
            } finally {
                msgReceiver.closeReceiving()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        val msgs = MultiReceiverChannel<TestMessage>()
        init {
            GlobalScope.launch {
                msgs.pipeToReceivers()
            }
        }
    }
}