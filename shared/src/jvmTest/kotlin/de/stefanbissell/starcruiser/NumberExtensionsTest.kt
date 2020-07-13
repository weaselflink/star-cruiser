package de.stefanbissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import kotlin.math.PI

class NumberExtensionsTest {

    @Test
    fun `converts to radians`() {
        expectThat(90.0.toRadians()).isNear(PI / 2)
        expectThat((-90.0).toRadians()).isNear(-PI / 2)
        expectThat(45.0.toRadians()).isNear(PI / 4)
        expectThat(30.0.toRadians()).isNear(PI / 6)
        expectThat(360.0.toRadians()).isNear(PI * 2)
    }

    @Test
    fun `converts to degrees`() {
        expectThat((PI / 2).toDegrees()).isNear(90.0)
        expectThat((-PI / 2).toDegrees()).isNear(-90.0)
        expectThat((PI / 4).toDegrees()).isNear(45.0)
        expectThat((PI / 6).toDegrees()).isNear(30.0)
        expectThat((PI * 2).toDegrees()).isNear(360.0)
    }

    @Test
    fun `converts to heading`() {
        expectThat(0.0.toHeading()).isNear(90.0)
        expectThat((-PI / 2).toHeading()).isNear(180.0)
        expectThat((PI / 4).toHeading()).isNear(45.0)
        expectThat((-PI / 4).toHeading()).isNear(135.0)
        expectThat((PI / 6).toHeading()).isNear(60.0)
        expectThat((PI * 2).toHeading()).isNear(90.0)
        expectThat((-PI).toHeading()).isNear(270.0)
        expectThat((-PI * 3).toHeading()).isNear(270.0)
        expectThat((PI * 3).toHeading()).isNear(270.0)
        expectThat((PI * 3 - PI / 6).toHeading()).isNear(300.0)
    }

    @Test
    fun `rounds to given digits`() {
        expectThat(PI.round(0)).isNear(3.0)
        expectThat(PI.round(2)).isNear(3.14)
        expectThat(PI.round(4)).isNear(3.1416)
        expectThat((-PI).round(0)).isNear(-3.0)
        expectThat((-PI).round(2)).isNear(-3.14)
        expectThat((-PI).round(4)).isNear(-3.1416)
        expectThat(0.0.round(0)).isNear(0.0)
        expectThat(0.0.round(4)).isNear(0.0)
    }

    @Test
    fun `formats to given digits`() {
        expectThat(23.0.format(2)).isEqualTo("23.00")
        expectThat((-23.0).format(2)).isEqualTo("-23.00")
        expectThat(23.1.format(2)).isEqualTo("23.10")
        expectThat(23.12.format(2)).isEqualTo("23.12")
        expectThat(23.125.format(2)).isEqualTo("23.13")
        expectThat(23.12576.format(2)).isEqualTo("23.13")
    }

    @Test
    fun `no padding if width greater than target`() {
        expectThat(456.pad(2)).isEqualTo("456")
        expectThat(1.pad(1)).isEqualTo("1")
    }

    @Test
    fun `no padding if target is invalid`() {
        expectThat(456.pad(0)).isEqualTo("456")
        expectCatching {
            1.pad(-5)
        }.isFailure()
    }

    @Test
    fun `adds padding if needed`() {
        expectThat(456.pad(4)).isEqualTo("0456")
        expectThat(1.pad(5)).isEqualTo("00001")
        expectThat(0.pad(3)).isEqualTo("000")
    }
}
