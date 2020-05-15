package de.bissell.starcruiser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
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
}