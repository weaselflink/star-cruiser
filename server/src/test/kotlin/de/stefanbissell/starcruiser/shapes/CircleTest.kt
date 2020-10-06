package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.p
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import kotlin.math.sqrt

class CircleTest {

    private val circle = Circle(
        center = p(50, 10),
        radius = 100
    )

    @Test
    fun `gives bounding box`() {
        expectThat(circle.boundingBox)
            .isEqualTo(Box(p(-50, -90), p(150, 110)))
    }

    @Test
    fun `recognizes points inside`() {
        expectThat(circle.isInside(p(50, 111))).isFalse()
        expectThat(circle.isInside(p(50, 109))).isTrue()
        expectThat(circle.isInside(p(-49, 10))).isTrue()
        expectThat(circle.isInside(p(-51, 10))).isFalse()
        expectThat(circle.isInside(diagonalFromCenter(1))).isTrue()
        expectThat(circle.isInside(diagonalFromCenter(99))).isTrue()
        expectThat(circle.isInside(diagonalFromCenter(101))).isFalse()
    }

    @Test
    fun `yields random point inside`() {
        repeat(10) {
            expectThat((circle.randomPointInside() - p(50, 10)).length())
                .isLessThan(100.1)
        }
    }

    private fun diagonalFromCenter(length: Number) =
        p(50, 10) + (p(length, length) / sqrt(2.0))
}
