@file:UseSerializers(BigDecimalSerializer::class)

package de.bissell.starcruiser

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.sin

@Serializable
data class Vector2(
    val x: BigDecimal = BigDecimal.ZERO,
    val y: BigDecimal = BigDecimal.ZERO
) {

    operator fun plus(other: Vector2): Vector2 =
        Vector2(x + other.x, y + other.y)

    operator fun times(other: BigDecimal): Vector2 =
        Vector2(x * other, y * other)

    fun setScale(scale: Int): Vector2 =
        Vector2(x.setScale(scale, RoundingMode.FLOOR), y.setScale(scale, RoundingMode.FLOOR))

    fun isZero() = x.isZero() && y.isZero()

    fun rotate(radians: BigDecimal) =
        if (isZero()) {
            Vector2()
        } else {
            Vector2(
                x = x * cos(radians.toDouble()) - y * sin(radians.toDouble()),
                y = x * sin(radians.toDouble()) + y * cos(radians.toDouble())
            )
        }

}

