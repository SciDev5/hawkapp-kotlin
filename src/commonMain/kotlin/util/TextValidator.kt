package util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class TextValidator(private val checks: Checks.() -> Unit) {
    @Serializable
    sealed interface Failure {
        fun failsOn(text: String): Boolean
    }

    @Serializable
    @SerialName("TooShortFailure")
    class TooShortFailure(val minLen: Int) : Failure {
        override fun failsOn(text: String) = text.length < minLen
    }

    @Serializable//(with = TooLongFailure.Serializer::class)
    @SerialName("TooLongFailure")
    class TooLongFailure(val maxL: Int) : Failure {
        override fun failsOn(text: String) = text.length > maxL

//        /** custom serializer because it wouldn't work by default so ¯\_(ツ)_/¯ */
//        class Serializer : KSerializer<TooLongFailure>  {
//            override val descriptor = PrimitiveSerialDescriptor("TooLongFailure", PrimitiveKind.INT)
//
//            override fun deserialize(decoder: Decoder) =
//                TooLongFailure(decoder.decodeInt())
//
//            override fun serialize(encoder: Encoder, value: TooLongFailure) {
//                encoder.encodeInt(value.maxLen)
//            }
//        }
    }
    @Serializable
    object UntrimmedFailure : Failure {
        override fun failsOn(text: String) = text.trim() != text
    }

    class Checks(
        private val text: String,
        private val failures: MutableSet<Failure>
    ) {
        private fun Failure.addIfFails() {
            if (failsOn(text))
                failures.add(this)
        }

        fun lengthAtLeast(len: Int) = TooShortFailure(len).addIfFails()
        fun lengthAtMost(len: Int) = TooLongFailure(len).addIfFails()
        fun lengthInRange(range: IntRange) {
            lengthAtLeast(range.first)
            lengthAtMost(range.last)
        }
        fun noBoundingWhitespace() = UntrimmedFailure.addIfFails()
    }

    operator fun invoke(text: String) = ValidityReport(
        mutableSetOf<Failure>().also { failureSet ->
            checks(Checks(text, failureSet))
        }
    )

    inner class ValidityReport(val failures: Set<Failure>) {
        val failed = failures.isNotEmpty()
    }
}