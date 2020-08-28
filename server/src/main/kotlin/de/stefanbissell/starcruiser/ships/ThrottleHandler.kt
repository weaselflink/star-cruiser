package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.clamp
import kotlin.math.abs

class ThrottleHandler(
    private val template: ShipTemplate,
) {

    var requested: Int = 0
        set(value) {
            field = value.clamp(-100, 100)
        }
    var actual: Double = 0.0
        set(value) {
            field = value.clamp(-100.0, 100.0)
        }

    fun update(time: GameTime) {
        val change = template.throttleResponsiveness * time.delta
        when {
            abs(actual - requested) <= change -> {
                actual = requested.toDouble()
            }
            requested > actual -> {
                actual += change
            }
            requested < actual -> {
                actual -= change
            }
        }
    }

    fun effectiveThrust(boostLevel: Double) =
        if (actual < 0) {
            actual * template.reverseThrustFactor * boostLevel
        } else {
            actual * template.aheadThrustFactor * boostLevel
        }
}
