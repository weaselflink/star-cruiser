package de.bissell.starcruiser

import de.bissell.starcruiser.MathDefaults.roundingMode
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class BigDecimalTest {

    @Test
    fun `0 rotation is 90 heading`() {
        expectThat(
            0.toBigDecimal().toHeading()
        ).isEqualTo(
            90.toBigDecimal().setScale(2, roundingMode)
        )
    }

    @Test
    fun `PI rotation is 270 heading`() {
        expectThat(
            PI.toHeading()
        ).isEqualTo(
            270.toBigDecimal().setScale(2, roundingMode)
        )
    }

    @Test
    fun `half PI rotation is 0 heading`() {
        expectThat(
            PI.divide(2.toBigDecimal(), 9, roundingMode).toHeading()
        ).isEqualTo(
            0.toBigDecimal().setScale(2, roundingMode)
        )
    }

    @Test
    fun `PI and a half rotation is 180 heading`() {
        expectThat(
            (PI * 3).divide(2.toBigDecimal(), 9, roundingMode).toHeading()
        ).isEqualTo(
            180.toBigDecimal().setScale(2, roundingMode)
        )
    }

    @Test
    fun `quarter PI rotation is 45 heading`() {
        expectThat(
            PI.divide(4.toBigDecimal(), 9, roundingMode).toHeading()
        ).isEqualTo(
            45.toBigDecimal().setScale(2, roundingMode)
        )
    }
}
