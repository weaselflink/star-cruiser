package de.stefanbissell.starcruiser

import kotlin.math.abs
import strikt.api.Assertion

fun Assertion.Builder<Double>.isNear(expected: Double, tolerance: Double = 0.0000001) =
    assert("is nearly equal to %s", expected) {
        val diff = abs(it - expected)
        if (diff < tolerance && diff < tolerance) pass() else fail()
    }

fun Assertion.Builder<Vector2>.isNear(expected: Vector2, tolerance: Double = 0.0000001) =
    assert("is nearly equal to %s", expected) {
        val diffX = abs(it.x - expected.x)
        val diffY = abs(it.y - expected.y)
        if (diffX < tolerance && diffY < tolerance) pass() else fail()
    }
