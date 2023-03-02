package util

abstract class IdSingletonMap<T : IdSingletonMap.HasId<Id>, Id, CreationData> {
    interface HasId<Id> {
        val id: Id
    }

    private val contentsMap = mutableMapOf<Id, T>()

    operator fun get(id: Id) = (
                contentsMap[id] ?: readFetch(id)?.also { contentsMap[id] = it }
            )?.also(this::alsoOnGet)

    fun create(dataIn: CreationData) =
        createFetch(dataIn).also {
            contentsMap[it.id] = it
        }

    fun forget(v: T) {
        if (forgetIfPossible(v))
            contentsMap.remove(v.id)
    }

    protected abstract fun readFetch(id: Id): T?
    protected abstract fun forgetIfPossible(v: T): Boolean
    protected abstract fun createFetch(data: CreationData): T

    protected open fun alsoOnGet(v: T) {}
}