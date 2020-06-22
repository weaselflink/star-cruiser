package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.LockStatus
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.time.Instant

class LockHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val targetId = ObjectId.random()
    private val lockingSpeed = 0.25
    private val lockHandler = LockHandler(targetId, lockingSpeed)

    @Test
    fun `starts with zero progress`() {
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

        expectThat(lockHandler.toMessage())
            .isA<LockStatus.InProgress>()
            .and {
                get { targetId }.isEqualTo(targetId)
                get { progress }.isNear(0.75)
            }
    }

    @Test
    fun `locked after sufficient time passed`() {
        stepTimeTo(5)

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
