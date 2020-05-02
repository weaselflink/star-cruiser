package de.bissell.starcruiser

import kotlinx.serialization.*
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode

object MathDefaults {
    val roundingMode: RoundingMode = RoundingMode.HALF_EVEN
}

fun BigDecimal.defaultScale(): BigDecimal =
    setScale(9, MathDefaults.roundingMode)

fun BigDecimal.clip(min: BigDecimal, max: BigDecimal) =
    min(max, max(min, this))

fun BigDecimal.clip(min: Long, max: Long) =
    min(max.toBigDecimal(), max(min.toBigDecimal(), this))

val PI: BigDecimal = BigDecimal.valueOf(kotlin.math.PI)

fun BigDecimal.toRadians(): BigDecimal =
    this.divide(180.toBigDecimal(), 9, MathDefaults.roundingMode) * PI

fun BigDecimal.toDegrees(): BigDecimal =
    this.divide(PI, 9, MathDefaults.roundingMode) * 180

operator fun BigDecimal.times(value: Long): BigDecimal =
    this * BigDecimal(value)

operator fun BigDecimal.times(value: Double): BigDecimal =
    this * BigDecimal(value)

fun BigDecimal.isZero() =
    this.compareTo(ZERO) == 0

fun min(a: BigDecimal, b: BigDecimal) =
    if (a > b) b else a

fun max(a: BigDecimal, b: BigDecimal) =
    if (a > b) a else b

fun BigDecimal.toHeading(): BigDecimal =
    (90.toBigDecimal() - this.toDegrees()).setScale(2, RoundingMode.HALF_EVEN).let {
        if (it < ZERO) {
            it + 360.toBigDecimal()
        } else {
            it
        }
    }

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
