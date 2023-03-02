package util.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UntilLock {
    private val blocker = Mutex(locked = true)

    private var locked = true
    fun unlock() {
        if (locked)
            blocker.unlock()
        locked = false
    }

    suspend fun wait() = if (locked) blocker.withLock { } else Unit
}