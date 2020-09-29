package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.p
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class BoxTest {

    private val box = Box(p(5, 10), p(15, 12))

    @Test
    fun `bounding box same as original`() {
        expectThat(box.boundingBox).isSameInstanceAs(box)
    }

    @Test
    fun `detects points inside box`() {
        expectThat(box.isInside(p(5, 10)))
        expectThat(box.isInside(p(15, 10)))
        expectThat(box.isInside(p(15, 12)))
        expectThat(box.isInside(p(5, 12)))
        expectThat(box.isInside(p(6, 11)))
        expectThat(box.isInside(p(14, 11)))
        expectThat(box.isInside(p(5.001, 10.001)))
    }

    @Test
    fun `detects points outside box`() {
        expectThat(box.isInside(p(4.999, 10)))
        expectThat(box.isInside(p(15.001, 10)))
        expectThat(box.isInside(p(15, 12.001)))
        expectThat(box.isInside(p(5, 12.001)))
        expectThat(box.isInside(p(0, 0)))
        expectThat(box.isInside(p(-1, -1)))
        expectThat(box.isInside(p(4.999, 11)))
    }
}
