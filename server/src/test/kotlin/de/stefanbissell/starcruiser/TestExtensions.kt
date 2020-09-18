package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipContactList
import strikt.api.Assertion
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan

fun Assertion.Builder<Double>.isNear(expected: Double, tolerance: Double = 0.0001): Assertion.Builder<Double> {
    return and {
        isGreaterThan(expected - tolerance)
        isLessThan(expected + tolerance)
    }
}

fun emptyContactList(ship: Ship) = ShipContactList(ship, emptyList())
