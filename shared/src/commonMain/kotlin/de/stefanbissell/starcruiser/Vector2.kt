package de.stefanbissell.starcruiser

import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
data class Vector2(
    val x: Double = 0.0,
    val y: Double = 0.0
) {

    constructor() : this(0.0, 0.0)

    constructor(
        x: Number = 0.0,
        y: Number = 0.0
    ) : this(x.toDouble(), y.toDouble())

    operator fun plus(other: Vector2): Vector2 =
        Vector2(x + other.x, y + other.y)

    operator fun minus(other: Vector2): Vector2 =
        Vector2(x - other.x, y - other.y)

    operator fun times(other: Double): Vector2 =
        Vector2(x * other, y * other)

    operator fun div(other: Double): Vector2 =
        Vector2(x / other, y / other)

    fun isZero() = x == 0.0 && y == 0.0

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

    fun normalize() =
        if (length() < 0.0001) {
            this
        } else {
            this * (1.0 / length())
        }

    fun angle() = atan2(y, x)

    companion object {
        fun random(maxLength: Number, minLength: Number = 0.001) =
            Vector2(
                x = Random.nextDouble() - 0.5,
                y = Random.nextDouble() - 0.5
            ).let {
                it.normalize() * Random.nextDouble(minLength.toDouble(), maxLength.toDouble())
            }
    }
}

fun p(x: Number, y: Number) = Vector2(x, y)

operator fun Double.times(other: Vector2): Vector2 = other * this

operator fun Vector2.times(other: Vector2): Double = (x * other.x) + (y * other.y)
