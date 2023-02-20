package data

import kotlinx.serialization.Serializable

@Serializable
class TimestampedId(val v: Long) : Comparable<TimestampedId> {

    constructor(
        randomPart: Int,
        timestampPart: Int
    ) : this(
        randomPart.toUInt().toLong() or
                timestampPart.toUInt().toULong().shl(32).toLong()
    )


    override fun compareTo(other: TimestampedId) =
        this.timestampPart.compareTo(other.timestampPart).takeIf { it != 0 }
            ?: this.randomPart.compareTo(other.randomPart)

    override fun equals(other: Any?) =
        other is TimestampedId && other.v == this.v

    // not collision proof but good enough
    override fun hashCode() = timestampPart xor randomPart


    val randomPart get() = (v and 0x0000_0000_FFFF_FFFF).toInt()

    /** Timestamp part in seconds since Unix Epoch */
    val timestampPart get() = ((v.toULong() and 0xFFFF_FFFF_0000_0000u) shr 32).toInt()

    override fun toString() = "TimestampedId_${
        v.toULong()
            .toString(16)
            .let { "0".repeat(16 - it.length) + it }
    }"
}