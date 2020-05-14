package de.bissell.starcruiser

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class Vector2(
    val x: Double = 0.0,
    val y: Double = 0.0
) {

    operator fun plus(other: Vector2): Vector2 =
        Vector2(x + other.x, y + other.y)

    operator fun minus(other: Vector2): Vector2 =
        Vector2(x - other.x, y - other.y)

    operator fun times(other: Double): Vector2 =
        Vector2(x * other, y * other)

    private fun isZero() = x == 0.0 && y == 0.0

    fun length(): Double =
        sqrt(x * x + y * y)

    fun rotate(radians: Double) =
        if (isZero()) {
            Vector2()
        } else {
            Vector2(
                x = x * cos(radians) - y * sin(radians),
                y = x * sin(radians) + y * cos(radians)
            )
        }
}
