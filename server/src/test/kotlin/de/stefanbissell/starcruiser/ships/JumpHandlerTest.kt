package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.JumpDriveMessage
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class JumpHandlerTest {

    private val time = GameTime.atEpoch()
    private val physicsEngine = mockk<PhysicsEngine>(relaxed = true)
    private val jumpDrive = JumpDrive()
    private var power = 1.0
    private val ship = PlayerShip(faction = TestFactions.player)
    private val jumpHandler = JumpHandler(jumpDrive, ship)

    @Test
    fun `ready initially`() {
        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Ready>()
    }

    @Test
    fun `jumping after jump initiated`() {
        jumpHandler.startJump()

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Jumping>()
            .get { progress }.isEqualTo(0.0)
    }

    @Test
    fun `updates progress when jumping`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 0.5)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Jumping>()
            .get { progress }.isNear(0.5)
    }

    @Test
    fun `reports jump complete`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.1)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { progress }.isNear(0.0)
        expectThat(jumpHandler.ready)
            .isFalse()
    }

    @Test
    fun `stats recharging after jump ended`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { progress }.isEqualTo(0.0)
        expectThat(jumpHandler.ready)
            .isFalse()
    }

    @Test
    fun `updates progress when recharging`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)
        stepTime(1.0 / jumpDrive.rechargeSpeed * 0.5)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { progress }.isNear(0.5)
        expectThat(jumpHandler.ready)
            .isFalse()
    }

    @Test
    fun `updates progress when recharging adjusted for low boost level`() {
        power = 0.5
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)
        stepTime(1.0 / jumpDrive.rechargeSpeed * 0.5)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { progress }.isNear(0.25)
        expectThat(jumpHandler.ready)
            .isFalse()
    }

    @Test
    fun `updates progress when recharging adjusted for high boost level`() {
        power = 1.5
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)
        stepTime(1.0 / jumpDrive.rechargeSpeed * 0.5)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { progress }.isNear(0.75)
        expectThat(jumpHandler.ready)
            .isFalse()
    }

    @Test
    fun `reports ready when recharged`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)
        stepTime(1.0 / jumpDrive.rechargeSpeed * 1.2)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Ready>()
        expectThat(jumpHandler.ready)
            .isTrue()
    }

    @Test
    fun `tracks jump animation`() {
        expectThat(jumpHandler.toMessage().animation)
            .isNull()

        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed - 1.0)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Jumping>()
            .get { animation }.isNotNull().isNear(-1.0)

        stepTime(0.2)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Jumping>()
            .get { animation }.isNotNull().isNear(-0.8)

        stepTime(0.8)

        verify(exactly = 1) {
            physicsEngine.jumpShip(ship.id, jumpHandler.jumpDistance)
        }

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { animation }.isNotNull().isNear(0.0)

        stepTime(0.6)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { animation }.isNotNull().isNear(0.6)

        stepTime(0.6)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { animation }.isNull()
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        jumpHandler.update(time, physicsEngine, power)
    }
}
