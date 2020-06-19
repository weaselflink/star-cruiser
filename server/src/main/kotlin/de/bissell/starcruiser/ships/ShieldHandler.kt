package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.ShieldMessage
import kotlin.math.max
import kotlin.math.min

class ShieldHandler(
    private val shieldTemplate: ShieldTemplate
) {

    private var up: Boolean = true
    private var damageSinceLastUpdate: Double = 0.0
    private var activated: Boolean = false

    private var currentStrength: Double = shieldTemplate.strength

    fun update(time: GameTime) {
        currentStrength = min(
            shieldTemplate.strength,
            currentStrength + shieldTemplate.rechargeSpeed * time.delta
        )
    }

    fun endUpdate() {
        if (currentStrength <= shieldTemplate.failureStrength) {
            up = false
        }
        activated = up && damageSinceLastUpdate > 0.0
        damageSinceLastUpdate = 0.0
    }

    fun takeDamageAndReportHullDamage(amount: Double): Double {
        return if (up) {
            val hullDamage = max(0.0, amount - currentStrength)
            damageSinceLastUpdate += amount
            currentStrength = max(
                0.0,
                currentStrength - amount
            )
            hullDamage
        } else {
            amount
        }
    }

    fun setUp(value: Boolean) {
        if (value) {
            if (currentStrength >= shieldTemplate.activationStrength) {
                up = true
            }
        } else {
            up = false
        }
    }

    fun toMessage() =
        ShieldMessage(
            radius = shieldTemplate.radius,
            up = up,
            activated = activated,
            strength = currentStrength,
            max = shieldTemplate.strength
        )
}