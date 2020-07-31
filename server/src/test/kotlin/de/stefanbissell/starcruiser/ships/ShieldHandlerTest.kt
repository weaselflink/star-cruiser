package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.time.Instant

class ShieldHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val shieldTemplate = ShieldTemplate()
    private var power = 1.0
    private val shieldHandler = ShieldHandler(shieldTemplate)

    @Test
    fun `starts with full shields in up state`() {
        expectThat(shieldHandler.toMessage())
            .and {
                get { up }.isTrue()
                get { strength }.isNear(shieldTemplate.strength)
            }
    }

    @Test
    fun `applies damage to shields and reports no hull damage if shields up`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0))
            .isEqualTo(0.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)
    }

    @Test
    fun `shields are activated after taking damage since last update`() {
        shieldHandler.takeDamageAndReportHullDamage(3.0)

        stepTimeTo(0.1)

        expectThat(shieldHandler.toMessage().activated)
            .isTrue()

        stepTimeTo(0.1)

        expectThat(shieldHandler.toMessage().activated)
            .isFalse()
    }

    @Test
    fun `recharges shields`() {
        shieldHandler.takeDamageAndReportHullDamage(3.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)

        stepTimeTo(5.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0 + shieldTemplate.rechargeSpeed * 5.0)
    }

    @Test
    fun `recharges shields adjusted for low boost level`() {
        power = 0.5
        shieldHandler.takeDamageAndReportHullDamage(3.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)

        stepTimeTo(5.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0 + shieldTemplate.rechargeSpeed * 5.0 * 0.5)
    }

    @Test
    fun `recharges shields adjusted for high boost level`() {
        power = 2.0
        shieldHandler.takeDamageAndReportHullDamage(3.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0)

        stepTimeTo(5.0)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 3.0 + shieldTemplate.rechargeSpeed * 5.0 * 2.0)
    }

    @Test
    fun `reports damage to hull if shields down`() {
        shieldHandler.setUp(false)

        expectThat(shieldHandler.takeDamageAndReportHullDamage(3.0))
            .isEqualTo(3.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength)
    }

    @Test
    fun `reports damage to hull if damage exceeds shields`() {
        expectThat(shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength + 3.0))
            .isEqualTo(3.0)
        expectThat(shieldHandler.toMessage().strength)
            .isNear(0.0)
    }

    @Test
    fun `shields go down if below failure strength`() {
        shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength - shieldTemplate.failureStrength + 3.0)

        stepTimeTo(0.1)

        expectThat(shieldHandler.toMessage().up)
            .isFalse()
    }

    @Test
    fun `shield cannot go up if below activation strength`() {
        shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength - shieldTemplate.activationStrength + 3.0)
        shieldHandler.setUp(false)
        shieldHandler.setUp(true)

        expectThat(shieldHandler.toMessage().up)
            .isFalse()
    }

    @Test
    fun `shield can go up after reaching activation strength`() {
        shieldHandler.takeDamageAndReportHullDamage(shieldTemplate.strength - shieldTemplate.activationStrength + 3.0)
        shieldHandler.setUp(false)

        stepTimeTo(4.0 / shieldTemplate.rechargeSpeed)

        shieldHandler.setUp(true)

        expectThat(shieldHandler.toMessage().up)
            .isTrue()
    }

    @Test
    fun `shield decays on low boost level`() {
        power = 0.05
        stepTimeTo(4.0 / shieldTemplate.decaySpeed)

        expectThat(shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 4.0)
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        shieldHandler.update(time, power)
        shieldHandler.endUpdate()
    }
}
