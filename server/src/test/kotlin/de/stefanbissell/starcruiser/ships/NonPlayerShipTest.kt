package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.ships.combat.DamageEvent
import de.stefanbissell.starcruiser.takeDamage
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class NonPlayerShipTest {

    private val time = GameTime.atEpoch()
    private val physicsEngine = mockk<PhysicsEngine>(relaxed = true)
    private val shieldTemplate = carrierTemplate.shield
    private val ship = NonPlayerShip(faction = TestFactions.enemy).apply {
        shieldHandler.modulation = 2
        beamHandlerContainer.modulation = 0
    }

    private var contactList = emptyList<Ship>()

    @Test
    fun `takes beam damage to shields`() {
        ship.takeDamage(PoweredSystemType.Reactor, 10.0, 0)

        expectThat(ship.shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 10.0)
    }

    @Test
    fun `takes torpedo damage to hull and all systems`() {
        ship.applyDamage(
            DamageEvent.Torpedo(ship.id, 7.0)
        )

        expectThat(ship.hull)
            .isNear(ship.template.hull - 7.0)
        expectThat(ship.powerHandler.poweredSystems.values)
            .all {
                get { damage }
                    .isNear(
                        (7.0 / PoweredSystemType.values().size) /
                            ship.template.poweredSystemDamageCapacity
                    )
            }
    }

    @Test
    fun `shields recharge`() {
        ship.takeDamage(PoweredSystemType.Reactor, 10.0, 0)
        stepTime(5)

        expectThat(ship.shieldHandler.toMessage().strength)
            .isNear(shieldTemplate.strength - 10.0 + shieldTemplate.rechargeSpeed * 5)
    }

    @Test
    fun `reactivates shields when possible and near threat`() {
        addHostileShip(p(100, 100))
        ship.takeDamage(PoweredSystemType.Reactor, shieldTemplate.strength, 0)
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
        ship.takeDamage(PoweredSystemType.Sensors, carrierTemplate.poweredSystemDamageCapacity * 0.2, 0)
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

    @Test
    fun `scans ships and stops when target destroyed`() {
        val targetId = addShip(p(100, 100)).id
        stepTime(1)

        expectThat(ship.scanHandler)
            .isNotNull()
            .get { targetId }.isEqualTo(targetId)

        ship.targetDestroyed(targetId)

        expectThat(ship.scanHandler)
            .isNull()
    }

    @Test
    fun `scans ships and stops when out of range`() {
        val targetId = addShip(p(100, 0)).id
        stepTime(0.1)

        expectThat(ship.scanHandler)
            .isNotNull()
            .get { targetId }.isEqualTo(targetId)

        contactList.first().position = p(ship.template.sensorRange * 2, 0)
        stepTime(0.1)

        expectThat(ship.scanHandler)
            .isNull()
    }

    @Test
    fun `updates scan progress`() {
        val targetId = addShip().id
        ship.startScan(targetId)

        expectThat(ship.scanHandler)
            .isNotNull()
            .get { progress }.isNear(0.0)

        stepTime(1)

        expectThat(ship.scanHandler)
            .isNotNull()
            .get { progress }.isNear(ship.template.scanSpeed)
    }

    @Test
    fun `aborts scan when target destroyed`() {
        val targetId = addShip().id
        ship.startScan(targetId)

        ship.targetDestroyed(targetId)

        expectThat(ship.scanHandler)
            .isNull()
    }

    @Test
    fun `updates lock progress`() {
        val targetId = addShip().id
        ship.startLock(targetId)

        expectThat(ship.lockHandler)
            .isNotNull()
            .get { progress }.isNear(0.0)

        stepTime(1)

        expectThat(ship.lockHandler)
            .isNotNull()
            .get { progress }.isNear(ship.template.lockingSpeed)
    }

    @Test
    fun `aborts lock when target destroyed`() {
        val targetId = addShip().id
        ship.startLock(targetId)

        ship.targetDestroyed(targetId)

        expectThat(ship.lockHandler)
            .isNull()
    }

    @Test
    fun `unscanned friendly ship is friendly`() {
        val target = addShip(faction = TestFactions.enemy)
        ship.scans[target.id] = ScanLevel.Basic

        expectThat(ship.getContactType(target))
            .isEqualTo(ContactType.Friendly)
    }

    @Test
    fun `unscanned hostile ship is unknown`() {
        val target = addShip()

        expectThat(ship.getContactType(target))
            .isEqualTo(ContactType.Unknown)
    }

    @Test
    fun `unscanned neutral ship is unknown`() {
        val target = addShip(faction = TestFactions.neutral)

        expectThat(ship.getContactType(target))
            .isEqualTo(ContactType.Unknown)
    }

    @Test
    fun `scanned friendly ship is friendly`() {
        val target = addShip(faction = TestFactions.enemy)
        ship.scans[target.id] = ScanLevel.Basic

        expectThat(ship.getContactType(target))
            .isEqualTo(ContactType.Friendly)
    }

    @Test
    fun `scanned hostile ship is enemy`() {
        val target = addShip(faction = TestFactions.player)
        ship.scans[target.id] = ScanLevel.Basic

        expectThat(ship.getContactType(target))
            .isEqualTo(ContactType.Enemy)
    }

    @Test
    fun `scanned neutral ship is neutral`() {
        val target = addShip(faction = TestFactions.neutral)
        ship.scans[target.id] = ScanLevel.Basic

        expectThat(ship.getContactType(target))
            .isEqualTo(ContactType.Neutral)
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds)
        ship.update(
            time = time,
            physicsEngine = physicsEngine,
            contactList = ContactList(ship, contactList)
        )
    }

    private fun addShip(
        position: Vector2 = p(100, 100),
        faction: de.stefanbissell.starcruiser.scenario.Faction = TestFactions.player
    ): Ship {
        val target = PlayerShip(
            position = position,
            faction = faction
        ).apply {
            shieldHandler.modulation = 2
        }
        contactList = listOf(target)
        return target
    }

    private fun addHostileShip(position: Vector2) {
        val target = addShip(position)
        ship.scans[target.id] = ScanLevel.Detailed
    }
}
