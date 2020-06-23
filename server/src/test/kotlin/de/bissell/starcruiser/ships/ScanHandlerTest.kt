package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.time.Instant

class ScanHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val targetId = ObjectId.random()
    private val scanningSpeed = 0.25
    private val scanHandler = ScanHandler(targetId, scanningSpeed)

    @Test
    fun `starts with zero progress`() {
        expectThat(scanHandler.isComplete).isFalse()
        expectThat(scanHandler.toMessage())
            .and {
                get { targetId }.isEqualTo(targetId)
                get { progress }.isNear(0.0)
            }
    }

    @Test
    fun `still in progress after insufficient time passed`() {
        stepTimeTo(3)

        expectThat(scanHandler.isComplete).isFalse()
        expectThat(scanHandler.toMessage())
            .and {
                get { targetId }.isEqualTo(targetId)
                get { progress }.isNear(0.75)
            }
    }

    @Test
    fun `locked after sufficient time passed`() {
        stepTimeTo(5)

        expectThat(scanHandler.isComplete).isTrue()
        expectThat(scanHandler.toMessage())
            .and {
                get { targetId }.isEqualTo(targetId)
                get { progress }.isNear(1.25)
            }
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        scanHandler.update(time)
    }
}