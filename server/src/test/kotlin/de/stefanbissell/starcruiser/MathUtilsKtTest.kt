package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import kotlin.math.PI
import kotlin.math.sqrt

class MathUtilsKtTest {

    @Test
    fun `calculates smallest signed angle`() {
        expectAngle(0.0, 0.0).isNear(0.0)
        expectAngle(PI, PI).isNear(0.0)
        expectAngle(0.0, PI - 0.1).isNear(PI - 0.1)
        expectAngle(0.0, PI + 0.1).isNear(-(PI - 0.1))
        expectAngle(0.0, 0.5).isNear(0.5)
        expectAngle(0.0, fullCircle - 0.5).isNear(-0.5)
        expectAngle(0.5, 0.0).isNear(-0.5)
        expectAngle(fullCircle - 0.5, 0.0).isNear(0.5)
        expectAngle(PI, fullCircle - 0.5).isNear(PI - 0.5)
        expectAngle(fullCircle - 0.5, PI).isNear(-(PI - 0.5))
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

    @Test
    fun `finds smallest positive in quadratic solution`() {
        expectThat(QuadraticResult.Imaginary.smallestPositive())
            .isNull()
        expectThat(QuadraticResult.One(-1.0).smallestPositive())
            .isNull()
        expectThat(QuadraticResult.One(0.0).smallestPositive())
            .isEqualTo(0.0)
        expectThat(QuadraticResult.One(1.0).smallestPositive())
            .isEqualTo(1.0)
        expectThat(QuadraticResult.Two(-1.0, -1.0).smallestPositive())
            .isNull()
        expectThat(QuadraticResult.Two(-1.0, 0.0).smallestPositive())
            .isEqualTo(0.0)
        expectThat(QuadraticResult.Two(0.0, -1.0).smallestPositive())
            .isEqualTo(0.0)
        expectThat(QuadraticResult.Two(1.0, -1.0).smallestPositive())
            .isEqualTo(1.0)
        expectThat(QuadraticResult.Two(1.0, 0.0).smallestPositive())
            .isEqualTo(0.0)
        expectThat(QuadraticResult.Two(2.0, 1.0).smallestPositive())
            .isEqualTo(1.0)
    }

    @Test
    fun `calculates intercept point`() {
        expectThat(
            interceptPoint(
                interceptorPosition = p(0, 0),
                interceptorSpeed = 1.0,
                targetPosition = p(1, 0),
                targetSpeed = p(1, 0)
            )
        ).isNull()

        expectThat(
            interceptPoint(
                interceptorPosition = p(0, 0),
                interceptorSpeed = 1.0,
                targetPosition = p(1, -1),
                targetSpeed = p(0, 1)
            )
        ).isNotNull()
            .isNear(p(1, 0))

        expectThat(
            interceptPoint(
                interceptorPosition = p(0, 0),
                interceptorSpeed = sqrt(2.0),
                targetPosition = p(1, -2),
                targetSpeed = p(0, 1)
            )
        ).isNotNull()
            .isNear(p(1, -1))

        expectThat(
            interceptPoint(
                interceptorPosition = p(0, 0),
                interceptorSpeed = 5.0,
                targetPosition = p(4, 0),
                targetSpeed = p(0, 3)
            )
        ).isNotNull()
            .isNear(p(4, 3))

        expectThat(
            interceptPoint(
                interceptorPosition = p(0, 0),
                interceptorSpeed = 5.0,
                targetPosition = p(4, 0),
                targetSpeed = p(0, 3)
            )
        ).isNotNull()
            .isNear(p(4, 3))

        expectThat(
            interceptPoint(
                interceptorPosition = p(5, 5),
                interceptorSpeed = 10.0,
                targetPosition = p(10, 10),
                targetSpeed = p(0, 0)
            )
        ).isNotNull()
            .isNear(p(10, 10))
    }

    private fun expectAngle(x: Double, y: Double) =
        expectThat(smallestSignedAngleBetween(x, y))
}
