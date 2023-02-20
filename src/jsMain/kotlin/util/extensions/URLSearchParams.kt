package util.extensions

import org.w3c.dom.url.URLSearchParams


@Suppress("UNUSED_VARIABLE")
val URLSearchParams.keys get():Set<String> {
    val keys = mutableSetOf<String>()
    val params = this
    val keysIterator = js("params.keys()")
    var iteratorNext = js("keysIterator.next()")
    var done: Boolean
    while (true) {
        done = js("iteratorNext.done") as Boolean
        val nextValue = js("iteratorNext.value") as? String
        if (done) break
        keys.add(nextValue!!)
        js("iteratorNext = keysIterator.next()")
    }
    return keys
}
val URLSearchParams.entries:Map<String,String?> get() = mapOf(*keys.map {
    it to get(it)!!
}.toTypedArray()).also {
    println(it.entries.joinToString(",") { v -> "[${v.key}:${v.value}]" })
}

fun decodeURIEntries(string: String) =
    URLSearchParams(decodeURIComponent(string))