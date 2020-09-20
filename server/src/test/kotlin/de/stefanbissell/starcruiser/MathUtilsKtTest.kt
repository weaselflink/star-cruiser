package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
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
        expectAngle(-PI * 0.5, PI * 0.4).isNear(PI * 0.9)
        expectAngle(PI * 0.5, -PI * 0.4).isNear(-PI * 0.9)
        expectAngle(PI * 0.9, -PI * 0.9).isNear(PI * 0.2)
        expectAngle(-PI * 0.9, PI * 0.9).isNear(-PI * 0.2)
        expectAngle(-PI * 0.9, -PI * 0.8).isNear(PI * 0.1)
        expectAngle(-PI * 0.8, -PI * 0.9).isNear(-PI * 0.1)
    }

    @Test
    fun `does not solve quadratic equation with imaginary solutions`() {
        expectThat(solveQuadratic(1.0, 1.0, 1.0))
            .isEqualTo(QuadraticResult.Imaginary)
    }

    @Test
    fun `solves quadratic equation with single solution`() {
        expectThat(solveQuadratic(1.0, 2.0, 1.0))
            .isEqualTo(QuadraticResult.One(-1.0))
    }

    @Test
    fun `solves quadratic equation with two solutions`() {
        expectThat(solveQuadratic(2.0, 5.0, 2.0))
            .isEqualTo(QuadraticResult.Two(-2.0 / 4.0, -8.0 / 4.0))
    }

    private fun expectAngle(x: Double, y: Double) =
        expectThat(smallestSignedAngleBetween(x, y))
}
