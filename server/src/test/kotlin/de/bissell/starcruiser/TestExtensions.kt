package de.bissell.starcruiser

import strikt.api.Assertion
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan

fun Assertion.Builder<Double>.isNear(expected: Double, tolerance: Double = 0.0001): Assertion.Builder<Double> {
    return and {
        isGreaterThan(expected - tolerance)
        isLessThan(expected + tolerance)
    }
}
