package util

class MutableSetMap<K,V> {
    private val map = mutableMapOf<K,MutableSet<V>>()

    fun add(k: K, v: V) {
        map[k] = get(k).also {
            it.add(v)
        }
    }
    operator fun get(k: K) =
        map[k] ?: mutableSetOf()

    fun remove(k: K) {
        map.remove(k)
    }
    fun remove(k: K, v: V) {
        map[k]?.let {
            it.remove(v)
            if (it.isEmpty())
                remove(k)
        }
    }
    fun clear() {
        map.clear()
    }
}