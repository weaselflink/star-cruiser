package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.Vector3
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.ships.BeamWeapon
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.PlayerShip
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class BeamHandlerTest {

    private val time = GameTime.atEpoch()
    private val beamWeapon = BeamWeapon(Vector3())
    private var power = 1.0
    private val ship = PlayerShip(faction = TestFactions.player)
    private var contactList: ContactList = ContactList(ship, emptyList())
    private val target = PlayerShip(faction = TestFactions.enemy).apply {
        position = p(0, 1000)
        setShieldsUp(false)
    }
    private var lockHandler: LockHandler? = null
    private val beamHandler = BeamHandlerContainer(listOf(beamWeapon), ship).beamHandlers.first()
    private val physicsEngine = mockk<PhysicsEngine>()

    @BeforeEach
    internal fun setUp() {
        every {
            physicsEngine.findObstructions(any(), any(), any())
        } returns emptyList()
    }

    @Test
    fun `initially idle`() {
        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `stays idle with no target locked`() {
        stepTime(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `stays idle with target locked but out of range`() {
        targetLockedAndOutOfRange()
        stepTime(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `stays idle with target locked in range but obstructed`() {
        targetLockedAndInRange()
        every {
            physicsEngine.findObstructions(any(), any(), any())
        } returns listOf(target.id)
        stepTime(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `fires when target in range`() {
        targetLockedAndInRange()
        stepTime(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Firing(0.0))
    }

    @Test
    fun `updates firing progress`() {
        targetLockedAndInRange()
        stepTime(1.0)
        stepTime(0.5)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Firing(0.5))
    }

    @Test
    fun `changes to recharging after firing`() {
        targetLockedAndInRange()
        stepTime(1.0)
        stepTime(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Recharging(0.0))
    }

    @Test
    fun `commences firing after recharging`() {
        targetLockedAndInRange()
        stepTime(1.0)
        stepTime(1.0)
        stepTime(5.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Firing(0.0))
    }

    @Test
    fun `aborts firing when target lock lost`() {
        targetLockedAndInRange()
        stepTime(1.0)
        stepTime(0.1)
        cancelLock()
        stepTime(0.1)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Recharging(0.0))
    }

    @Test
    fun `deals damage to target`() {
        targetLockedAndInRange()
        stepTime(1.0)
        stepTime(1.0)

        expectThat(target.hull)
            .isEqualTo(target.template.hull - 1.0)
    }

    private fun cancelLock() {
        contactList = ContactList(ship, emptyList())
        lockHandler = null
    }

    private fun targetLockedAndInRange() {
        targetInRange()
        targetLocked()
    }

    private fun targetLockedAndOutOfRange() {
        targetOutOfRange()
        targetLocked()
    }

    private fun targetLocked() {
        val lockTime = GameTime.atEpoch().apply {
            update(10)
        }
        lockHandler = LockHandler(target.id, 1.0).apply {
            update(lockTime)
        }
        contactList = ContactList(ship, listOf(target))
    }

    private fun targetInRange() {
        target.position = p(0, 50)
    }

    private fun targetOutOfRange() {
        target.position = p(0, 1000)
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        beamHandler.update(time, power, contactList, lockHandler, physicsEngine)
    }
}
