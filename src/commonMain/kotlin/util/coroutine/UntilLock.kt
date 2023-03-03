package util.coroutine

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UntilLock {
    private val blocker = Mutex(locked = true)

    var locked = true
        private set
    fun unlock() {
        if (locked)
            blocker.unlock()
        locked = false
    }

    suspend fun wait() = if (locked) blocker.withLock { } else Unit

    @OptIn(DelicateCoroutinesApi::class)
    fun invokeOnUnlock(block:()->Unit) = GlobalScope.launch {
        wait()
        block()
    }
}