package de.bissell.starcruiser

import kotlinx.serialization.*
import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.constrain(min: BigDecimal, max: BigDecimal) =
    min(max, max(min, this))

val PI: BigDecimal = BigDecimal.valueOf(kotlin.math.PI)

fun BigDecimal.toRadians(): BigDecimal =
    this.divide(BigDecimal(180), 9, RoundingMode.HALF_UP) * PI

operator fun BigDecimal.times(value: Long): BigDecimal =
    this * BigDecimal(value)

operator fun BigDecimal.times(value: Double): BigDecimal =
    this * BigDecimal(value)

fun BigDecimal.isZero() =
    this.compareTo(BigDecimal.ZERO) == 0

fun min(a: BigDecimal, b: BigDecimal) =
    if (a > b) b else a

fun max(a: BigDecimal, b: BigDecimal) =
    if (a > b) a else b

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}
