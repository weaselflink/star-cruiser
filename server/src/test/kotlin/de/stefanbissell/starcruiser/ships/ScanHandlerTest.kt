package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import java.time.Instant

class ScanHandlerTest {

    private val time = GameTime(Instant.EPOCH)
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
        val noiseOnStandardPower = scanHandler.noise
        power = 0.5
        expectThat(scanHandler.noise)
            .isGreaterThan(noiseOnStandardPower)
    }

    @Test
    fun `decrease noise when given more power`() {
        val noiseOnStandardPower = scanHandler.noise
        power = 1.5
        expectThat(scanHandler.noise)
            .isLessThan(noiseOnStandardPower)
    }

    @Test
    fun `can solve game but is not complete`() {
        solveGame()
        expectThat(scanHandler.noise)
            .isLessThan(0.05)
        expectThat(scanHandler.isComplete)
            .isFalse()
    }

    @Test
    fun `can solve game and complete game`() {
        solveGame()
        stepTimeTwoSeconds()
        expectThat(scanHandler.isComplete)
            .isTrue()
    }

    private fun stepTimeTwoSeconds() {
        time.update(2.0)
        scanHandler.update(time)
    }

    private fun solveGame() {
        (0 until 2).forEach { dimension: Int ->
            (0..100).map {
                it / 100.0
            }.map {
                scanHandler.adjustInput(dimension, it)
                it to scanHandler.noise
            }.minByOrNull {
                it.second
            }?.also {
                scanHandler.adjustInput(dimension, it.first)
            }
        }
    }

    private fun createScanHandler(): ScanHandler {
        var candidate = ScanHandler(targetId) { power }
        while (candidate.noise >= 1.0) {
            candidate = ScanHandler(targetId) { power }
        }
        return candidate
    }

    private fun ScanHandler.toMessage() =
        toMessage { PlayerShip(designation = "dummy") }

    private val ScanHandler.noise
        get() = toMessage().noise
}
