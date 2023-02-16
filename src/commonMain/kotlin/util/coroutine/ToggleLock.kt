package util.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ToggleLock(startLocked: Boolean) {
    private val blocker = Mutex(locked = startLocked)
    private val lockStateSynchronization = Mutex()

    suspend fun lock() {
        lockStateSynchronization.withLock {
            blocker.tryLock()
        }
    }

    suspend fun unlock() {
        lockStateSynchronization.withLock {
            if (blocker.isLocked)
                blocker.unlock()
        }
    }

    suspend fun setLocked(beLocked: Boolean) =
        if (beLocked)
            lock()
        else unlock()

    suspend fun wait() {
        blocker.lock() // blocking lock
        unlock() // checked unlock (prevent race condition)
    }
}