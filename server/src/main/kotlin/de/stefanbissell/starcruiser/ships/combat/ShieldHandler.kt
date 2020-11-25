package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.moduloDistance
import de.stefanbissell.starcruiser.ships.ShieldTemplate
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.max
import kotlin.random.Random

class ShieldHandler(
    private val shieldTemplate: ShieldTemplate
) {

    var modulation: Int = Random.nextInt(7)
        set(value) {
            field = value.clamp(0, 7)
        }
    var up: Boolean = true
        set(value) {
            if (value) {
                if (activationAllowed()) {
                    field = true
                }
            } else {
                field = false
            }
        }
    var timeSinceLastDamage: Double = 1_000_000.0
    private var activated: Boolean = false

    var currentStrength: Double = shieldTemplate.strength
        set(value) {
            field = value.clamp(0.0, shieldTemplate.strength)
        }
    val strengthRatio: Double
        get() = currentStrength / shieldTemplate.strength

    fun update(time: GameTime, boostLevel: Double = 1.0) {
        activated = false
        timeSinceLastDamage += time.delta
        currentStrength += rechargeAmount(time, boostLevel)
    }

    fun takeDamageAndReportHullDamage(amount: Double, modulation: Int): Double {
        timeSinceLastDamage = 0.0
        return if (up) {
            val effectiveDamage = amount * modulationMultiplier(modulation)
            takeDamageToShieldAndThenHull(effectiveDamage)
        } else {
            amount
        }
    }

    fun takeDamageAndReportHullDamage(beamDamageEvent: DamageEvent.Beam): Double {
        timeSinceLastDamage = 0.0
        return if (up) {
            val effectiveDamage = beamDamageEvent.amount * modulationMultiplier(beamDamageEvent.modulation)
            takeDamageToShieldAndThenHull(effectiveDamage)
        } else {
            beamDamageEvent.amount
        }
    }

    fun toggleUp() {
        up = !up
    }

    fun activationAllowed() = currentStrength >= shieldTemplate.activationStrength

    fun toMessage() =
        ShieldMessage(
            radius = shieldTemplate.radius,
            up = up,
            activated = activated,
            strength = currentStrength.twoDigits(),
            max = shieldTemplate.strength,
            modulation = modulation
        )

    private fun modulationMultiplier(beamModulation: Int) =
        when (moduloDistance(modulation, beamModulation, 8)) {
            0 -> 0.5
            1 -> 0.75
            2 -> 1.0
            3 -> 1.5
            else -> 2.0
        }

    private fun shieldFailing() = currentStrength <= shieldTemplate.failureStrength

    private fun rechargeAmount(time: GameTime, boostLevel: Double) =
        if (boostLevel > 0.1) {
            shieldTemplate.rechargeSpeed * time.delta * boostLevel
        } else {
            -shieldTemplate.decaySpeed * time.delta
        }

    private fun takeDamageToShieldAndThenHull(amount: Double): Double {
        val hullDamage = max(0.0, amount - currentStrength)
        activated = up
        currentStrength -= amount
        if (shieldFailing()) {
            up = false
        }
        return hullDamage
    }
}
