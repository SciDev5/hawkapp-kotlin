package util

fun Long.bigEndianBytes() = arrayOfNulls<Byte>(8).mapIndexed { i, _ ->
    (this shr (8 * (7 - i))).toByte()
}.toByteArray()

fun ByteArray.bigEndianLong(startIndex: Int) =
    slice(startIndex until (startIndex + 8))
        .mapIndexed { i, v ->
            v.toUByte().toLong() shl (8 * (7 - i))
        }.reduce { a, b -> a or b }