package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.time.Instant

class ScanHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val targetId = ObjectId.random()
    private var power = 1.0
    private val scanHandler = createScanHandler()

    @Test
    fun `starts with unsolved game`() {
        expectThat(scanHandler.isComplete).isFalse()
        expectThat(scanHandler.toMessage())
            .and {
                get { targetId }.isEqualTo(targetId)
                get { designation }.isEqualTo("dummy")
            }
    }

    @Test
    fun `increases noise when given less power`() {
        val noiseOnStandardPower = scanHandler.toMessage().noise
        power = 0.5
        expectThat(scanHandler.toMessage().noise)
            .isGreaterThan(noiseOnStandardPower)
    }

    @Test
    fun `decrease noise when given more power`() {
        val noiseOnStandardPower = scanHandler.toMessage().noise
        power = 1.5
        expectThat(scanHandler.toMessage().noise)
            .isLessThan(noiseOnStandardPower)
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        scanHandler.update(time)
    }

    private fun createScanHandler(): ScanHandler {
        var candidate = ScanHandler(targetId) { power }
        while (candidate.toMessage().noise >= 1.0) {
            candidate = ScanHandler(targetId) { power }
        }
        return candidate
    }

    private fun ScanHandler.toMessage() =
        toMessage { Ship(designation = "dummy") }
}
