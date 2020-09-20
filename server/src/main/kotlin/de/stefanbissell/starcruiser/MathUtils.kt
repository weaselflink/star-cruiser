package de.stefanbissell.starcruiser

import kotlin.math.PI
import kotlin.math.sqrt

fun smallestSignedAngleBetween(source: Number, target: Number): Double {
    val x = source.toDouble()
    val y = target.toDouble()
    val a = (y - x)
    return if (a >= -PI && a <= PI) {
        a
    } else {
        if (a > PI) {
            -(2 * PI - a)
        } else {
            a + 2 * PI
        }
    }
}

fun solveQuadratic(a: Double, b: Double, c: Double): QuadraticResult {
    val rootBody = b * b - 4 * a * c
    return when {
        rootBody < 0.0 -> QuadraticResult.Imaginary
        rootBody == 0.0 -> QuadraticResult.One(-b / (2 * a))
        else -> QuadraticResult.Two(
            (-b + sqrt(rootBody)) / (2 * a),
            (-b - sqrt(rootBody)) / (2 * a)
        )
    }
}

sealed class QuadraticResult {

    object Imaginary : QuadraticResult()

    data class One(val value: Double) : QuadraticResult()

    data class Two(val first: Double, val second: Double) : QuadraticResult()

    fun smallestPositive() =
        when (this) {
            is Imaginary -> null
            is One -> if (value >= 0) value else null
            is Two -> {
                listOf(first, second)
                    .filter { it >= 0 }
                    .minOrNull()
            }
        }
}
