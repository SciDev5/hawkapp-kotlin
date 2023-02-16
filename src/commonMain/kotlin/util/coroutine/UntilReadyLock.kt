package util.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UntilReadyLock {
    private val blocker = Mutex(locked = true)

    fun unlock() {
        if (blocker.isLocked)
            blocker.unlock()
    }

    suspend fun wait() = blocker.withLock { }
}