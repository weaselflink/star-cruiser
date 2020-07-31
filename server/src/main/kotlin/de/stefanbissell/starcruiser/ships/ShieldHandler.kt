package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.max

class ShieldHandler(
    private val shieldTemplate: ShieldTemplate
) {

    private var up: Boolean = true
    private var damageSinceLastUpdate: Double = 0.0
    private var activated: Boolean = false

    private var currentStrength: Double = shieldTemplate.strength
        set(value) {
            field = value.clamp(0.0, shieldTemplate.strength)
        }

    fun update(time: GameTime, boostLevel: Double) {
        currentStrength += rechargeAmount(time, boostLevel)
    }

    fun endUpdate() {
        if (shieldFailing()) {
            up = false
        }
        activated = up && damageSinceLastUpdate > 0.0
        damageSinceLastUpdate = 0.0
    }

    fun takeDamageAndReportHullDamage(amount: Double): Double {
        return if (up) {
            takeDamageToShieldAndThenHull(amount)
        } else {
            amount
        }
    }

    fun setUp(value: Boolean) {
        if (value) {
            if (activationAllowed()) {
                up = true
            }
        } else {
            up = false
        }
    }

    private fun activationAllowed() = currentStrength >= shieldTemplate.activationStrength

    fun toMessage() =
        ShieldMessage(
            radius = shieldTemplate.radius,
            up = up,
            activated = activated,
            strength = currentStrength.twoDigits(),
            max = shieldTemplate.strength
        )

    private fun shieldFailing() = currentStrength <= shieldTemplate.failureStrength

    private fun rechargeAmount(time: GameTime, boostLevel: Double) =
        if (boostLevel > 0.1) {
            shieldTemplate.rechargeSpeed * time.delta * boostLevel
        } else {
            -shieldTemplate.decaySpeed * time.delta
        }

    private fun takeDamageToShieldAndThenHull(amount: Double): Double {
        val hullDamage = max(0.0, amount - currentStrength)
        damageSinceLastUpdate += amount
        currentStrength -= amount
        return hullDamage
    }
}
