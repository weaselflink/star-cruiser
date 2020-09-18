package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import kotlin.math.PI

class MathUtilsKtTest {

    @Test
    fun `calculates smallest signed angle`() {
        expectAngle(0.0, 0.0).isNear(0.0)
        expectAngle(PI, PI).isNear(0.0)
        expectAngle(0.0, PI - 0.1).isNear(PI - 0.1)
        expectAngle(0.0, PI + 0.1).isNear(-(PI - 0.1))
        expectAngle(0.0, 0.5).isNear(0.5)
        expectAngle(0.0, 2 * PI - 0.5).isNear(-0.5)
        expectAngle(0.5, 0.0).isNear(-0.5)
        expectAngle(2 * PI - 0.5, 0.0).isNear(0.5)
        expectAngle(PI, 2 * PI - 0.5).isNear(PI - 0.5)
        expectAngle(2 * PI - 0.5, PI).isNear(-(PI - 0.5))
    }

    private fun expectAngle(x: Double, y: Double) =
        expectThat(smallestSignedAngleBetween(x, y))
}
