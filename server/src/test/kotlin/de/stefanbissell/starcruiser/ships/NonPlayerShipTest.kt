package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.time.Instant

class NonPlayerShipTest {

    private val time = GameTime(Instant.EPOCH)
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
        stepTime(5)

        expectThat(ship.shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 10.0 + shieldTemplate.rechargeSpeed * 5)
    }

    @Test
    fun `reactivates shields when possible`() {
        ship.takeDamage(PoweredSystemType.Reactor, shieldTemplate.strength)
        stepTime(0.01)
        expectThat(ship.shieldHandler.toMessage().up)
            .isFalse()

        val timeToActivationStrength = shieldTemplate.activationStrength / shieldTemplate.rechargeSpeed
        stepTime(timeToActivationStrength)
        stepTime(timeToActivationStrength * 0.2)
        expectThat(ship.shieldHandler.toMessage().up)
            .isTrue()
    }

    @Test
    fun `repairs damaged system`() {
        ship.takeDamage(PoweredSystemType.Sensors, shieldTemplate.strength)
        ship.takeDamage(PoweredSystemType.Sensors, carrierTemplate.poweredSystemDamageCapacity * 0.2)
        expectThat(ship.powerHandler.poweredSystems[PoweredSystemType.Sensors])
            .isNotNull()
            .get { damage }.isNear(0.2)
        stepTime(0.01)

        expectThat(ship.powerHandler.repairing)
            .isTrue()

        stepTime(1.1 / carrierTemplate.repairSpeed)
        expectThat(ship.powerHandler.repairing)
            .isFalse()
        expectThat(ship.powerHandler.poweredSystems[PoweredSystemType.Sensors])
            .isNotNull()
            .get { damage }.isEqualTo(0.0)
    }

    private fun stepTime(seconds: Number, shipProvider: ShipProvider = { null }): ShipUpdateResult {
        time.update(seconds.toDouble())
        ship.update(time, physicsEngine, shipProvider)
        return ship.endUpdate(physicsEngine)
    }
}
