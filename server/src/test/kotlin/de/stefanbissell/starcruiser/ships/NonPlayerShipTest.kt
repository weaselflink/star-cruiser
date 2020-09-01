package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
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

    private var contactList = emptyList<Ship>()

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
    fun `reactivates shields when possible and near threat`() {
        addHostileShip(p(100, 100))
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
        ship.shieldHandler.toggleUp()
        ship.takeDamage(PoweredSystemType.Sensors, carrierTemplate.poweredSystemDamageCapacity * 0.2)
        expectThat(ship.powerHandler.poweredSystems[PoweredSystemType.Sensors])
            .isNotNull()
            .get { damage }.isNear(0.2)
        stepTime(0.01)

        expectThat(ship.powerHandler.repairing)
            .isTrue()

        stepTime(carrierTemplate.poweredSystemDamageCapacity * 0.21 / carrierTemplate.repairSpeed)
        expectThat(ship.powerHandler.repairing)
            .isFalse()
        expectThat(ship.powerHandler.poweredSystems[PoweredSystemType.Sensors])
            .isNotNull()
            .get { damage }.isEqualTo(0.0)
    }

    private fun stepTime(seconds: Number, shipProvider: ShipProvider = { null }): ShipUpdateResult {
        time.update(seconds.toDouble())
        ship.update(
            time = time,
            physicsEngine = physicsEngine,
            contactList = contactList,
            shipProvider = shipProvider
        )
        return ship.endUpdate(physicsEngine)
    }

    private fun addHostileShip(position: Vector2) {
        val target = PlayerShip(position = position)
        contactList = listOf(target)
        ship.scans[target.id] = ScanLevel.Detailed
    }
}
