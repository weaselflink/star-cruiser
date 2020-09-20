package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import kotlin.math.PI
import kotlin.math.sqrt

class Vector2Test {

    @Test
    fun `zero vector is zero`() {
        expectThat(Vector2().isZero())
            .isTrue()
    }

    @Test
    fun `length of zero vector is zero`() {
        expectThat(Vector2().length())
            .isEqualTo(0.0)
    }

    @Test
    fun `computes length`() {
        expectThat(Vector2(3, 4).length())
            .isNear(5)
        expectThat(Vector2(4, 3).length())
            .isNear(5)
        expectThat(Vector2(1, 1).length())
            .isNear(sqrt(2.0))
    }

    @Test
    fun `adds vectors`() {
        expectThat(Vector2(5, 1.1) + Vector2(-1, 0.9))
            .isNear(Vector2(4, 2))
        expectThat(Vector2(-1, 0.9) + Vector2(5, 1.1))
            .isNear(Vector2(4, 2))
    }

    @Test
    fun `subtracts vectors`() {
        expectThat(Vector2(5, 1.1) - Vector2(-1, 0.9))
            .isNear(Vector2(6, 0.2))
    }

    @Test
    fun `scales vectors`() {
        expectThat(Vector2(5, 1.1) * 2.5)
            .isNear(Vector2(12.5, 2.75))
        expectThat(2.5 * Vector2(5, 1.1))
            .isNear(Vector2(12.5, 2.75))
    }

    @Test
    fun `dot product`() {
        expectThat(Vector2(5, 1.1) * Vector2(5, 1.1))
            .isNear(25 + 1.21)
        expectThat(Vector2(5, 1.1) * Vector2(-2, -1.1))
            .isNear(-10 - 1.21)
        expectThat(Vector2(0, 0) * Vector2(2, -1.1))
            .isNear(0)
    }

    @Test
    fun `rotates vectors`() {
        expectThat(Vector2(1, 1).rotate(PI / 2))
            .isNear(Vector2(-1, 1))
        expectThat(Vector2(1, 1).rotate(-PI / 2))
            .isNear(Vector2(1, -1))
        expectThat(Vector2(1, 1).rotate(PI / 4))
            .isNear(Vector2(0, sqrt(2.0)))
    }
}
