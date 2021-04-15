package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class TimedScanHandlerTest {

    private val time = GameTime.atEpoch()
    private val scanningSpeed = 0.1
    private val scanHandler = TimedScanHandler(ObjectId.random(), scanningSpeed)

    @Test
    fun `not completed initially`() {
        expectThat(scanHandler.isComplete)
            .isFalse()
    }

    @Test
    fun `not completed after insufficient time elapsed`() {
        stepTime(9)

        expectThat(scanHandler.isComplete)
            .isFalse()
    }

    @Test
    fun `completed after sufficient time elapsed`() {
        stepTime(11)

        expectThat(scanHandler.isComplete)
            .isTrue()
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds)
        scanHandler.update(time)
    }
}
