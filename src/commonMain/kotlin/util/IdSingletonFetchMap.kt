package util

abstract class IdSingletonFetchMap<T : IdSingletonMap.HasId<Id>, Id> {

    private val contentsMap = mutableMapOf<Id, T>()

    suspend fun get(id: Id) = contentsMap[id]
        ?: readFetch(id)?.also { contentsMap[id] = it }

    fun forget(v: T) {
        if (forgetIfPossible(v))
            contentsMap.remove(v.id)
    }

    protected abstract suspend fun readFetch(id: Id): T?
    protected abstract fun forgetIfPossible(v: T): Boolean
}