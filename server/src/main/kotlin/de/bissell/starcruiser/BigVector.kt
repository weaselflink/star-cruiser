package de.bissell.starcruiser

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.cos
import kotlin.math.sin

data class BigVector(
    val x: BigDecimal = BigDecimal.ZERO,
    val y: BigDecimal = BigDecimal.ZERO
) {

    operator fun plus(other: BigVector): BigVector =
        BigVector(x + other.x, y + other.y)

    operator fun minus(other: BigVector): BigVector =
        BigVector(x - other.x, y - other.y)

    operator fun times(other: BigDecimal): BigVector =
        BigVector(x * other, y * other)

    fun setScale(scale: Int): BigVector =
        BigVector(
            x.setScale(scale, MathDefaults.roundingMode),
            y.setScale(scale, MathDefaults.roundingMode)
        )

    fun isZero() = x.isZero() && y.isZero()

    fun rotate(radians: BigDecimal) =
        if (isZero()) {
            BigVector()
        } else {
            BigVector(
                x = x * cos(radians.toDouble()) - y * sin(radians.toDouble()),
                y = x * sin(radians.toDouble()) + y * cos(radians.toDouble())
            )
        }

    fun length(): BigDecimal =
        (x * x + y * y).sqrt(MathContext.DECIMAL32)

    fun toVector2() = Vector2(
        x = x.toDouble(),
        y = y.toDouble()
    )
}

