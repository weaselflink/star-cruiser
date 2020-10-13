package de.stefanbissell.starcruiser

import kotlin.math.PI
import kotlin.math.sqrt

const val fullCircle = 2 * PI

fun smallestSignedAngleBetween(source: Number, target: Number): Double {
    val x = source.toDouble()
    val y = target.toDouble()
    val a = (y - x)
    return if (a >= -PI && a <= PI) {
        a
    } else {
        if (a > PI) {
            -(fullCircle - a)
        } else {
            a + fullCircle
        }
    }
}

fun interceptPoint(
    interceptorPosition: Vector2,
    interceptorSpeed: Double,
    targetPosition: Vector2,
    targetSpeed: Vector2
): Vector2? {
    val relativePosition = targetPosition - interceptorPosition
    val a = (targetSpeed * targetSpeed) - (interceptorSpeed * interceptorSpeed)
    val b = 2 * (relativePosition * targetSpeed)
    val c = relativePosition * relativePosition
    val interceptTime = solveQuadratic(a, b, c).smallestPositive()

    return interceptTime?.let {
        targetPosition + (targetSpeed * it)
    }
}

fun solveQuadratic(a: Double, b: Double, c: Double): QuadraticResult {
    if (a == 0.0) {
        return solveLinear(b, c)
    }
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

fun solveLinear(b: Double, c: Double): QuadraticResult {
    return if (b == 0.0) {
        QuadraticResult.Imaginary
    } else {
        QuadraticResult.One(-c / b)
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
