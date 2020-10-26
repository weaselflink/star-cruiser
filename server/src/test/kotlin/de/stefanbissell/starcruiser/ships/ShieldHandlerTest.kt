package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isTrue

class ShieldHandlerTest {

    private val time = GameTime.atEpoch()
    private val shieldTemplate = ShieldTemplate()
    private var power = 1.0
    private val shieldHandler = ShieldHandler(shieldTemplate).apply { modulation = 2 }

    @Test
    fun `starts with random modulation`() {
        expectThat(shieldHandler.modulation)
            .isGreaterThanOrEqualTo(0)
            .isLessThanOrEqualTo(7)
    }

    @Test
    fun `starts with full shields in up state`() {
        expectThat(shieldHandler) {
            get { up }.isTrue()
            get { currentStrength }.isNear(shieldTemplate.strength)
        }
    }

    @Test
    fun `applies damage to shields and reports no hull damage if shields up`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 0))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)
    }

    @Test
    fun `shields are activated after taking damage since last update`() {
        shieldHandler.takeDamageAndReportHullDamage(3.0, 0)

        stepTime(0.1)

        expectThat(shieldHandler.toMessage().activated)
            .isTrue()

        stepTime(0.1)

        expectThat(shieldHandler.toMessage().activated)
            .isFalse()
    }

    @Test
    fun `tracks time since last damage`() {
        shieldHandler.takeDamageAndReportHullDamage(3.0, 0)

        stepTime(1.0)

        expectThat(shieldHandler.timeSinceLastDamage)
            .isEqualTo(0.0)

        stepTime(2.5)

        expectThat(shieldHandler.timeSinceLastDamage)
            .isEqualTo(2.5)

        stepTime(2.5)

        expectThat(shieldHandler.timeSinceLastDamage)
            .isEqualTo(5.0)
    }

    @Test
    fun `recharges shields`() {
        shieldHandler.takeDamageAndReportHullDamage(3.0, 0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)

        stepTime(5.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0 + shieldTemplate.rechargeSpeed * 5.0)
    }

    @Test
    fun `recharges shields adjusted for low boost level`() {
        power = 0.5
        shieldHandler.takeDamageAndReportHullDamage(3.0, 0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)

        stepTime(5.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0 + shieldTemplate.rechargeSpeed * 5.0 * 0.5)
    }

    @Test
    fun `recharges shields adjusted for high boost level`() {
        power = 2.0
        shieldHandler.takeDamageAndReportHullDamage(3.0, 0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)

        stepTime(5.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0 + shieldTemplate.rechargeSpeed * 5.0 * 2.0)
    }

    @Test
    fun `reports damage to hull if shields down`() {
        shieldHandler.up = false

        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 0))
            .isEqualTo(3.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength)
    }

    @Test
    fun `reports damage to hull if damage exceeds shields`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength + 3.0, 0))
            .isEqualTo(3.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(0.0)
    }

    @Test
    fun `shields go down if below failure strength`() {
        shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength - shieldTemplate.failureStrength + 3.0, 0)

        stepTime(0.1)

        expectThat(shieldHandler.toMessage().up)
            .isFalse()
    }

    @Test
    fun `shield cannot go up if below activation strength`() {
        shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength - shieldTemplate.activationStrength + 3.0, 0)
        shieldHandler.up = false
        shieldHandler.up = true

        expectThat(shieldHandler.toMessage().up)
            .isFalse()
    }

    @Test
    fun `shield can go up after reaching activation strength`() {
        shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength - shieldTemplate.activationStrength + 3.0, 0)
        shieldHandler.up = false

        stepTime(4.0 / shieldTemplate.rechargeSpeed)

        shieldHandler.up = true

        expectThat(shieldHandler.toMessage().up)
            .isTrue()
    }

    @Test
    fun `shield decays on low boost level`() {
        power = 0.05
        stepTime(4.0 / shieldTemplate.decaySpeed)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 4.0)
    }

    @Test
    fun `calculates strength ratio`() {
        shieldHandler.currentStrength = shieldTemplate.strength * 1.0
        expectThat(shieldHandler.strengthRatio)
            .isEqualTo(1.0)

        shieldHandler.currentStrength = shieldTemplate.strength * 0.0
        expectThat(shieldHandler.strengthRatio)
            .isEqualTo(0.0)

        shieldHandler.currentStrength = shieldTemplate.strength * 0.1
        expectThat(shieldHandler.strengthRatio)
            .isNear(0.1)

        shieldHandler.currentStrength = shieldTemplate.strength * 0.01
        expectThat(shieldHandler.strengthRatio)
            .isNear(0.01)
    }

    @Test
    fun `halves damage when modulation matches beams`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 2))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 1.5)
    }

    @Test
    fun `three quarter damage when modulation near beams`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 3))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 2.25)
    }

    @Test
    fun `normal damage when modulation slightly mismatches beams`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 4))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)
    }

    @Test
    fun `one and a half damage when modulation strongly mismatches beams`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 5))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 4.5)
    }

    @Test
    fun `doubles damage when modulation completely mismatches beams`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0, 6))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 6.0)
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        shieldHandler.update(time, power)
        shieldHandler.endUpdate(time)
    }
}
