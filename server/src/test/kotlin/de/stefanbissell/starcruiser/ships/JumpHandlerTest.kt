package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.JumpDriveMessage
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.time.Instant

class JumpHandlerTest {

    private val time = GameTime(Instant.EPOCH)
    private val jumpDrive = JumpDrive()
    private var power = 1.0
    private val jumpHandler = JumpHandler(jumpDrive)

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
        expectThat(jumpHandler.jumpComplete)
            .isFalse()
    }

    @Test
    fun `reports jump complete`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Jumping>()
            .get { progress }.isNear(1.2)
        expectThat(jumpHandler.jumpComplete)
            .isTrue()
    }

    @Test
    fun `stats recharging after jump ended`() {
        jumpHandler.startJump()

        stepTime(1.0 / jumpDrive.jumpingSpeed * 1.2)

        jumpHandler.endJump()

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { progress }.isEqualTo(0.0)
        expectThat(jumpHandler.ready)
            .isFalse()
    }

    @Test
    fun `updates progress when recharging`() {
        jumpHandler.startJump()
        jumpHandler.endJump()

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
        jumpHandler.endJump()

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
        jumpHandler.endJump()

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
        jumpHandler.endJump()

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

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Jumping>()
            .get { animation }.isNotNull().isNear(0.0)

        jumpHandler.endJump()

        stepTime(0.2)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { animation }.isNotNull().isNear(0.2)

        stepTime(1.2)

        expectThat(jumpHandler.toMessage())
            .isA<JumpDriveMessage.Recharging>()
            .get { animation }.isNull()
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        jumpHandler.update(time, power)
    }
}
