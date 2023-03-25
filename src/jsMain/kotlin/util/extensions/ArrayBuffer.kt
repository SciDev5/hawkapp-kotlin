package util.extensions

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

fun ByteArray.toArrayBuffer() =
    ArrayBuffer(size).also {
        val view = Uint8Array(it)
        for (i in indices)
            view[i] = this[i]
    }