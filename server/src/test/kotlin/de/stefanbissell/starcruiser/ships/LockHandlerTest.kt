package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.time.Instant

class LockHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val targetId = ObjectId.random()
    private val lockingSpeed = 0.25
    private var power = 1.0
    private val lockHandler = LockHandler(targetId, lockingSpeed) { power }

    @Test
    fun `starts with zero progress`() {
        expectThat(lockHandler.isComplete).isFalse()
        expectThat(lockHandler.toMessage())
            .isA<LockStatus.InProgress>()
            .and {
                get { targetId }.isEqualTo(targetId)
                get { progress }.isNear(0.0)
            }
    }

    @Test
    fun `still in progress after insufficient time passed`() {
        stepTimeTo(3)

        expectThat(lockHandler.isComplete).isFalse()
        expectThat(lockHandler.toMessage())
            .isA<LockStatus.InProgress>()
            .and {
                get { targetId }.isEqualTo(targetId)
                get { progress }.isNear(lockingSpeed * 3)
            }
    }

    @Test
    fun `applies power level to progress`() {
        power = 1.5
        stepTimeTo(2)

        expectThat(lockHandler.toMessage())
            .isA<LockStatus.InProgress>()
            .and {
                get { progress }.isNear(lockingSpeed * 2 * power)
            }
    }

    @Test
    fun `locked after sufficient time passed`() {
        stepTimeTo(5)

        expectThat(lockHandler.isComplete).isTrue()
        expectThat(lockHandler.toMessage())
            .isA<LockStatus.Locked>()
            .and {
                get { targetId }.isEqualTo(targetId)
            }
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        lockHandler.update(time)
    }
}
