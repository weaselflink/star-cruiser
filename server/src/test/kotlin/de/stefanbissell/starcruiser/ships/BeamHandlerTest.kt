package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PhysicsEngine
import de.stefanbissell.starcruiser.Vector3
import de.stefanbissell.starcruiser.p
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant

class BeamHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val beamWeapon = BeamWeapon(Vector3())
    private var power = 1.0
    private val ship = Ship()
    private var shipProvider: (ObjectId) -> Ship? = { null }
    private val target = Ship().apply {
        position = p(0, 1000)
        setShieldsUp(false)
    }
    private var lockHandler: LockHandler? = null
    private val beamHandler = BeamHandler(beamWeapon, ship)
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
        stepTimeTo(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `stays idle with target locked but out of range`() {
        targetLockedAndOutOfRange()
        stepTimeTo(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `stays idle with target locked in range but obstructed`() {
        targetLockedAndInRange()
        every {
            physicsEngine.findObstructions(any(), any(), any())
        } returns listOf(ObjectId.random())
        stepTimeTo(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Idle)
    }

    @Test
    fun `fires when target in range`() {
        targetLockedAndInRange()
        stepTimeTo(1.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Firing(0.0))
    }

    @Test
    fun `updates firing progress`() {
        targetLockedAndInRange()
        stepTimeTo(1.0)
        stepTimeTo(1.5)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Firing(0.5))
    }

    @Test
    fun `changes to recharging after firing`() {
        targetLockedAndInRange()
        stepTimeTo(1.0)
        stepTimeTo(2.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Recharging(0.0))
    }

    @Test
    fun `commences firing after recharging`() {
        targetLockedAndInRange()
        stepTimeTo(1.0)
        stepTimeTo(2.0)
        stepTimeTo(7.0)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Firing(0.0))
    }

    @Test
    fun `aborts firing when target lock lost`() {
        targetLockedAndInRange()
        stepTimeTo(1.0)
        stepTimeTo(1.1)
        cancelLock()
        stepTimeTo(1.2)

        expectThat(beamHandler.toMessage(lockHandler).status)
            .isEqualTo(BeamStatus.Recharging(0.0))
    }

    @Test
    fun `deals damage to target`() {
        targetLockedAndInRange()
        stepTimeTo(1.0)
        stepTimeTo(2.0)

        expectThat(target.toMessage().hull)
            .isEqualTo(target.template.hull - 1.0)
    }

    private fun cancelLock() {
        shipProvider = { null }
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
        val lockTime = GameTime().apply {
            update(Instant.EPOCH)
        }
        lockTime.update(Instant.EPOCH.plusSeconds(10))
        lockHandler = LockHandler(ObjectId.random(), 1.0) { 1.0 }.apply {
            update(lockTime)
        }
        shipProvider = { target }
    }

    private fun targetInRange() {
        target.position = p(0, 50)
    }

    private fun targetOutOfRange() {
        target.position = p(0, 1000)
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        beamHandler.update(time, power, shipProvider, lockHandler, physicsEngine)
    }
}
