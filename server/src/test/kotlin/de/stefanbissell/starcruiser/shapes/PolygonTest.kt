package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.p
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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

    @TestFactory
    fun `recognizes points inside triangle`() =
        listOf(
            p(0.1, 0.1),
            p(0.5, 0.5),
            p(1, 2),
            p(4, 0.5)
        ).map {
            dynamicTest(
                "p(${it.x}, ${it.y}) is inside triangle"
            ) {
                expectThat(triangle.isInside(it)).isTrue()
            }
        }

    @TestFactory
    fun `recognizes points outside triangle`() =
        listOf(
            p(-0.1, 0.1),
            p(0.1, -0.1),
            p(-0.5, 0.5),
            p(0.5, -0.5),
            p(3, 2),
            p(6, 1)
        ).map {
            dynamicTest(
                "p(${it.x}, ${it.y}) is outside triangle"
            ) {
                expectThat(triangle.isInside(it)).isFalse()
            }
        }

    @TestFactory
    fun `recognizes points inside horseshoe`() =
        listOf(
            p(2, 2),
            p(2, 4),
            p(6, 2),
            p(6, 4)
        ).map {
            dynamicTest(
                "p(${it.x}, ${it.y}) is inside horseshoe"
            ) {
                expectThat(horseshoe.isInside(it)).isTrue()
            }
        }

    @TestFactory
    fun `recognizes points outside horseshoe`() =
        listOf(
            p(0, 0),
            p(0, 4),
            p(2, 6),
            p(4, 4),
            p(6, 6),
            p(8, 4),
            p(8, 4),
            p(6, 0),
            p(2, 0)
        ).map {
            dynamicTest(
                "p(${it.x}, ${it.y}) is outside horseshoe"
            ) {
                expectThat(horseshoe.isInside(it)).isFalse()
            }
        }

    @Test
    fun `yields random point inside`() {
        repeat(10) {
            expectThat(triangle.isInside(triangle.randomPointInside())).isTrue()
        }
        repeat(10) {
            expectThat(horseshoe.isInside(horseshoe.randomPointInside())).isTrue()
        }
    }
}
