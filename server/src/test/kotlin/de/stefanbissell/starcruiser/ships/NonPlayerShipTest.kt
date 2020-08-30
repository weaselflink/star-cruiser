package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.time.Instant

class NonPlayerShipTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val physicsEngine = mockk<PhysicsEngine>(relaxed = true)
    private val shieldTemplate = carrierTemplate.shield
    private val ship = NonPlayerShip()

    @Test
    fun `takes damage to shields`() {
        ship.takeDamage(PoweredSystemType.Reactor, 10.0)

        expectThat(ship.shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 10.0)
    }

    @Test
    fun `shields recharge`() {
        ship.takeDamage(PoweredSystemType.Reactor, 10.0)
        stepTimeTo(5)

        expectThat(ship.shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 10.0 + shieldTemplate.rechargeSpeed * 5)
    }

    @Test
    fun `reactivates shields when possible`() {
        ship.takeDamage(PoweredSystemType.Reactor, shieldTemplate.strength)
        stepTimeTo(0.01)
        expectThat(ship.shieldHandler.toMessage().up)
            .isFalse()

        val timeToActivationStrength = shieldTemplate.activationStrength / shieldTemplate.rechargeSpeed
        stepTimeTo(timeToActivationStrength)
        stepTimeTo(timeToActivationStrength * 1.2)
        expectThat(ship.shieldHandler.toMessage().up)
            .isTrue()
    }

    private fun stepTimeTo(seconds: Number, shipProvider: ShipProvider = { null }): ShipUpdateResult {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        ship.update(time, physicsEngine, shipProvider)
        return ship.endUpdate(physicsEngine)
    }
}
