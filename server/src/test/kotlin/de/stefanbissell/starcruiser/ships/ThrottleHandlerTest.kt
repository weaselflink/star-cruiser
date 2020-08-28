package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import java.time.Instant

class ThrottleHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val throttleHandler = ThrottleHandler(carrierTemplate)

    @Test
    fun `updates actual up to requested value`() {
        throttleHandler.requested = 50

        stepTimeTo(1)
        expectThat(throttleHandler.actual)
            .isNear(carrierTemplate.throttleResponsiveness)

        stepTimeTo(10)
        expectThat(throttleHandler.actual)
            .isNear(50.0)
    }

    @Test
    fun `updates actual down to requested value`() {
        throttleHandler.requested = -50

        stepTimeTo(1)
        expectThat(throttleHandler.actual)
            .isNear(-carrierTemplate.throttleResponsiveness)

        stepTimeTo(10)
        expectThat(throttleHandler.actual)
            .isNear(-50.0)
    }

    @Test
    fun `calculates ahead thrust`() {
        throttleHandler.requested = 50

        stepTimeTo(10)
        expectThat(throttleHandler.effectiveThrust(1.0))
            .isNear(50 * carrierTemplate.aheadThrustFactor)
        expectThat(throttleHandler.effectiveThrust(0.2))
            .isNear(0.2 * 50 * carrierTemplate.aheadThrustFactor)
        expectThat(throttleHandler.effectiveThrust(1.2))
            .isNear(1.2 * 50 * carrierTemplate.aheadThrustFactor)
    }

    @Test
    fun `calculates reverse thrust`() {
        throttleHandler.requested = -50

        stepTimeTo(10)
        expectThat(throttleHandler.effectiveThrust(1.0))
            .isNear(-50 * carrierTemplate.reverseThrustFactor)
        expectThat(throttleHandler.effectiveThrust(0.2))
            .isNear(0.2 * -50 * carrierTemplate.reverseThrustFactor)
        expectThat(throttleHandler.effectiveThrust(1.2))
            .isNear(1.2 * -50 * carrierTemplate.reverseThrustFactor)
    }

    private fun stepTimeTo(seconds: Number) {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        throttleHandler.update(time)
    }
}
