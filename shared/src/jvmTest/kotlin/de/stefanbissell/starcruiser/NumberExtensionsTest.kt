package de.stefanbissell.starcruiser

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import kotlin.math.PI

class NumberExtensionsTest {

    @TestFactory
    fun `converts to radians`(): List<DynamicTest> =
        listOf(
            90.0 to PI / 2,
            -90.0 to -PI / 2,
            45.0 to PI / 4,
            30.0 to PI / 6,
            360.0 to PI * 2
        ).map { (degrees, radians) ->
            DynamicTest.dynamicTest(
                "$degrees degrees converts to $radians in radians"
            ) {
                expectThat(degrees.toRadians()).isNear(radians)
            }
        }

    @TestFactory
    fun `converts to degrees`(): List<DynamicTest> =
        listOf(
            PI / 2 to 90.0,
            -PI / 2 to -90.0,
            PI / 4 to 45.0,
            PI / 6 to 30.0,
            PI * 2 to 360.0
        ).map { (radians, degrees) ->
            DynamicTest.dynamicTest(
                "$radians radians converts to $degrees in degrees"
            ) {
                expectThat(radians.toDegrees()).isNear(degrees)
            }
        }

    @TestFactory
    fun `converts to heading`(): List<DynamicTest> =
        listOf(
            0.0 to 90.0,
            -PI / 2 to 180.0,
            PI / 4 to 45.0,
            -PI / 4 to 135.0,
            PI / 6 to 60.0,
            PI * 2 to 90.0,
            -PI to 270.0,
            -PI * 3 to 270.0,
            PI * 3 to 270.0,
            PI * 3 - PI / 6 to 300.0
        ).map { (radians, heading) ->
            DynamicTest.dynamicTest(
                "$radians radians converts to heading of $heading"
            ) {
                expectThat(radians.toHeading()).isNear(heading)
            }
        }

    @TestFactory
    fun `converts to rounded heading`(): List<DynamicTest> =
        listOf(
            0.49 to 90,
            0.51 to 89,
            89.51 to 0,
            90.49 to 0,
            90.51 to 359
        ).map { (degrees, heading) ->
            DynamicTest.dynamicTest(
                "$degrees degrees converts to rounded heading of $heading"
            ) {
                expectThat(degrees.toRadians().toIntHeading()).isEqualTo(heading)
            }
        }

    @TestFactory
    fun `rounds to given digits`(): List<DynamicTest> =
        listOf(
            PI to 0 gives 3.0,
            PI to 2 gives 3.14,
            PI to 4 gives 3.1416,
            -PI to 0 gives -3.0,
            -PI to 2 gives -3.14,
            -PI to 4 gives -3.1416,
            0.0 to 0 gives 0.0,
            0.0 to 4 gives 0.0
        ).map { (unrounded, digits, rounded) ->
            DynamicTest.dynamicTest(
                "$unrounded rounded to $digits digits gives $rounded"
            ) {
                expectThat(unrounded.round(digits)).isNear(rounded)
            }
        }

    @TestFactory
    fun `formats to given digits`(): List<DynamicTest> =
        listOf(
            23.0 to 2 gives "23.00",
            -23.0 to 2 gives "-23.00",
            23.1 to 2 gives "23.10",
            23.12 to 2 gives "23.12",
            23.125 to 2 gives "23.13",
            23.12576 to 2 gives "23.13",
            23.12576 to 4 gives "23.1258",
            23.12576 to 0 gives "23"
        ).map { (unrounded, digits, formatted) ->
            DynamicTest.dynamicTest(
                "$unrounded formatted with $digits digits gives $formatted"
            ) {
                expectThat(unrounded.format(digits)).isEqualTo(formatted)
            }
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

    @TestFactory
    fun `add thousands separators`(): List<DynamicTest> =
        listOf(
            1 to "1",
            567 to "567",
            4567 to "4,567",
            -4567 to "-4,567",
            34567 to "34,567",
            234567 to "234,567",
            1234567 to "1,234,567"
        ).map { (number, withThousands) ->
            DynamicTest.dynamicTest(
                "$number with thousands separator gives $withThousands"
            ) {
                expectThat(number.formatThousands()).isEqualTo(withThousands)
            }
        }

    private infix fun <A, B, C> Pair<A, B>.gives(c: C) = Triple(first, second, c)
}
