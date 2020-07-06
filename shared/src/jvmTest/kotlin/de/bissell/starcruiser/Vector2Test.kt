package de.bissell.starcruiser

import kotlin.math.PI
import kotlin.math.sqrt
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

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
        expectThat(Vector2(3.0, 4.0).length())
            .isNear(5.0)
        expectThat(Vector2(4.0, 3.0).length())
            .isNear(5.0)
        expectThat(Vector2(1.0, 1.0).length())
            .isNear(sqrt(2.0))
    }

    @Test
    fun `adds vectors`() {
        expectThat(Vector2(5.0, 1.1) + Vector2(-1.0, 0.9))
            .isNear(Vector2(4.0, 2.0))
        expectThat(Vector2(-1.0, 0.9) + Vector2(5.0, 1.1))
            .isNear(Vector2(4.0, 2.0))
    }

    @Test
    fun `subtracts vectors`() {
        expectThat(Vector2(5.0, 1.1) - Vector2(-1.0, 0.9))
            .isNear(Vector2(6.0, 0.2))
    }

    @Test
    fun `scales vectors`() {
        expectThat(Vector2(5.0, 1.1) * 2.5)
            .isNear(Vector2(12.5, 2.75))
        expectThat(2.5 * Vector2(5.0, 1.1))
            .isNear(Vector2(12.5, 2.75))
    }

    @Test
    fun `rotates vectors`() {
        expectThat(Vector2(1.0, 1.0).rotate(PI / 2))
            .isNear(Vector2(-1.0, 1.0))
        expectThat(Vector2(1.0, 1.0).rotate(-PI / 2))
            .isNear(Vector2(1.0, -1.0))
        expectThat(Vector2(1.0, 1.0).rotate(PI / 4))
            .isNear(Vector2(0.0, sqrt(2.0)))
    }
}
