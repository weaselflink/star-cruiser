package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.DynamicObject
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.combat.DamageEvent
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import strikt.api.Assertion
import strikt.assertions.isEqualTo

fun Assertion.Builder<Double>.isNear(expected: Number, tolerance: Double = 0.0000001) =
    isEqualTo(expected.toDouble(), tolerance)

fun Assertion.Builder<Vector2>.isNear(expected: Vector2, tolerance: Double = 0.0000001) =
    and {
        get { x }.isEqualTo(expected.x, tolerance)
        get { y }.isEqualTo(expected.y, tolerance)
    }

fun emptyContactList(ship: Ship) = ContactList(ship, emptyList())

fun DynamicObject.takeDamage(targetSystemType: PoweredSystemType, amount: Double, modulation: Int) {
    applyDamage(DamageEvent.Beam(id, targetSystemType, amount, modulation))
}

suspend fun expectWithTimeout(timeMillis: Long = 1000L, block: suspend () -> Unit) {
    var exception: Throwable? = null
    try {
        exception = withTimeout(timeMillis) {
            repeatUntilNoException(block)
        }
    } catch (ex: Throwable) {
        if (ex is TimeoutCancellationException && exception != null) {
            throw exception
        } else {
            throw ex
        }
    }
}

private suspend fun repeatUntilNoException(block: suspend () -> Unit): Throwable? {
    var exception: Throwable? = null
    var waiting = true
    while (waiting) {
        try {
            block()
            waiting = false
            exception = null
        } catch (ex: Throwable) {
            exception = ex
        }
        delay(100)
    }
    return exception
}
