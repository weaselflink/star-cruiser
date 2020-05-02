package de.bissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.math.BigDecimal

class Vector2Test {

    @Test
    fun `adds to other vector`() {
        expectThat(
            Vector2(BigDecimal("12.5"), BigDecimal("0.2")) + Vector2(BigDecimal("1000.00"), BigDecimal("0.001"))
        ).isEqualTo(
            Vector2(BigDecimal("1012.50"), BigDecimal("0.201"))
        )
    }

    @Test
    fun `multiplies with scalar greater one`() {
        expectThat(
            Vector2(BigDecimal("12.5"), BigDecimal("0.2")) * BigDecimal(5)
        ).isEqualTo(
            Vector2(BigDecimal("62.5"), BigDecimal("1.0"))
        )
    }

    @Test
    fun `multiplies with scalar less than one`() {
        expectThat(
            Vector2(BigDecimal("12.5"), BigDecimal("0.2")) * BigDecimal("0.002")
        ).isEqualTo(
            Vector2(BigDecimal("0.0250"), BigDecimal("0.0004"))
        )
    }

    @Test
    fun `sets scale of vector`() {
        expectThat(
            Vector2(BigDecimal("12"), BigDecimal("0.2050000")).setScale(2)
        ).isEqualTo(
            Vector2(BigDecimal("12.00"), BigDecimal("0.20"))
        )
    }

    @Test
    fun `detects zero regardless of scale`() {
        expectThat(
            Vector2(BigDecimal("0"), BigDecimal("0.000")).isZero()
        ).isTrue()
    }

    @Test
    fun `rotates vector by 90 degrees`() {
        expectThat(
            Vector2(BigDecimal(1), BigDecimal(1)).rotate(BigDecimal(90).toRadians()).setScale(6)
        ).isEqualTo(
            Vector2(BigDecimal("-1.000000"), BigDecimal("1.000000"))
        )
    }

    @Test
    fun `rotates vector by minus 90 degrees`() {
        expectThat(
            Vector2(BigDecimal(1), BigDecimal(1)).rotate(BigDecimal(-90).toRadians()).setScale(6)
        ).isEqualTo(
            Vector2(BigDecimal("1.000000"), BigDecimal("-1.000000"))
        )
    }

    @Test
    fun `rotates vector by 45 degrees`() {
        expectThat(
            Vector2(BigDecimal(1), BigDecimal(1)).rotate(BigDecimal(45).toRadians()).setScale(6)
        ).isEqualTo(
            Vector2(BigDecimal("0.000000"), BigDecimal("1.414214"))
        )
    }

    @Test
    fun `rotates vector by 0 degrees`() {
        expectThat(
            Vector2(BigDecimal(1), BigDecimal(1)).rotate(BigDecimal(0))
        ).isEqualTo(
            Vector2(BigDecimal(1), BigDecimal(1))
        )
    }
}
