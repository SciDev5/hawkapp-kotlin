package util.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KPromise<T> {
    private val lock = Mutex(locked = true)
    private var resolution: T? = null
    fun resolve(value: T) {
        resolution = value
        if (lock.isLocked)
            lock.unlock()
    }

    suspend fun await(): T = lock.withLock {
        resolution!!
    }
}

fun KPromise<Unit>.resolve() = resolve(Unit)
