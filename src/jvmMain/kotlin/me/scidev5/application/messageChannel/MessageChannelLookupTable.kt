package me.scidev5.application.messageChannel

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@OptIn(DelicateCoroutinesApi::class)
sealed class MessageChannelLookupTable {



    object DM : MessageChannelLookupTable() {

    }

    companion object {
        val coroutineScope = GlobalScope
    }
}