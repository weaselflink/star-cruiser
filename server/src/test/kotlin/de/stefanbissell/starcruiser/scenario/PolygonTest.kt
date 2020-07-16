package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.p
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class PolygonTest {

    private val triangle = Polygon(
        listOf(
            p(6, 0),
            p(0, 0),
            p(0, 3)
        )
    )

    private val horseshoe = Polygon(
        listOf(
            p(1, 1),
            p(1, 5),
            p(3, 5),
            p(3, 3),
            p(5, 3),
            p(5, 5),
            p(7, 5),
            p(7, 1)
        )
    )

    @Test
    fun `gives triangle bounding box`() {
        expectThat(triangle.boundingBox)
            .isEqualTo(Box(p(0, 0), p(6, 3)))
    }

    @Test
    fun `gives horseshoe bounding box`() {
        expectThat(horseshoe.boundingBox)
            .isEqualTo(Box(p(1, 1), p(7, 5)))
    }

    @Test
    fun `recognizes points inside triangle`() {
        expectThat(triangle.isInside(p(0.1, 0.1))).isTrue()
        expectThat(triangle.isInside(p(0.5, 0.5))).isTrue()
        expectThat(triangle.isInside(p(1, 2))).isTrue()
        expectThat(triangle.isInside(p(4, 0.5))).isTrue()
    }

    @Test
    fun `recognizes points outside triangle`() {
        expectThat(triangle.isInside(p(-0.1, 0.1))).isFalse()
        expectThat(triangle.isInside(p(0.1, -0.1))).isFalse()
        expectThat(triangle.isInside(p(-0.5, 0.5))).isFalse()
        expectThat(triangle.isInside(p(0.5, -0.5))).isFalse()
        expectThat(triangle.isInside(p(3, 2))).isFalse()
        expectThat(triangle.isInside(p(6, 1))).isFalse()
    }

    @Test
    fun `recognizes points inside horseshoe`() {
        expectThat(horseshoe.isInside(p(2, 2))).isTrue()
        expectThat(horseshoe.isInside(p(2, 4))).isTrue()
        expectThat(horseshoe.isInside(p(6, 2))).isTrue()
        expectThat(horseshoe.isInside(p(6, 4))).isTrue()
    }

    @Test
    fun `recognizes points outside horseshoe`() {
        expectThat(horseshoe.isInside(p(0, 0))).isFalse()
        expectThat(horseshoe.isInside(p(0, 4))).isFalse()
        expectThat(horseshoe.isInside(p(2, 6))).isFalse()
        expectThat(horseshoe.isInside(p(4, 4))).isFalse()
        expectThat(horseshoe.isInside(p(6, 6))).isFalse()
        expectThat(horseshoe.isInside(p(8, 4))).isFalse()
        expectThat(horseshoe.isInside(p(8, 4))).isFalse()
        expectThat(horseshoe.isInside(p(6, 0))).isFalse()
        expectThat(horseshoe.isInside(p(2, 0))).isFalse()
    }
}
