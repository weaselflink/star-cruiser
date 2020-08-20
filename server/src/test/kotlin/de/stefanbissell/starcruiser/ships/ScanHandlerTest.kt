package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.time.Instant

class ScanHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val targetId = ObjectId.random()
    private var power = 1.0
    private val scanHandler = ScanHandler(targetId) { power }

    @Test
    fun `starts with unsolved game`() {
        expectThat(scanHandler.isComplete).isFalse()
        expectThat(scanHandler.toMessage { Ship(designation = "dummy") })
            .and {
                get { targetId }.isEqualTo(targetId)
                get { designation }.isEqualTo("dummy")
            }
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        scanHandler.update(time)
    }
}
