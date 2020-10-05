package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.p
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import kotlin.math.sqrt

class RingTest {

    private val ring = Ring(
        center = p(50, 10),
        outer = 100.0,
        inner = 50.0
    )

    @Test
    fun `gives bounding box`() {
        expectThat(ring.boundingBox)
            .isEqualTo(Box(p(-50, -90), p(150, 110)))
    }

    @Test
    fun `recognizes points inside`() {
        expectThat(ring.isInside(p(1, 10))).isFalse()
        expectThat(ring.isInside(p(0, 10))).isTrue()
        expectThat(ring.isInside(p(-50, 10))).isTrue()
        expectThat(ring.isInside(p(-51, 10))).isFalse()
        expectThat(ring.isInside(diagonalFromCenter(49))).isFalse()
        expectThat(ring.isInside(diagonalFromCenter(51))).isTrue()
        expectThat(ring.isInside(diagonalFromCenter(99))).isTrue()
        expectThat(ring.isInside(diagonalFromCenter(101))).isFalse()
    }

    @Test
    fun `yields random point inside`() {
        repeat(10) {
            expectThat((ring.randomPointInside() - p(50, 10)).length())
                .isGreaterThan(49.9)
                .isLessThan(100.1)
        }
    }

    private fun diagonalFromCenter(length: Number) =
        p(50, 10) + (p(length, length) / sqrt(2.0))
}
