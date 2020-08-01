package de.stefanbissell.starcruiser

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

const val degreesToRadians = 0.017453292519943295
const val radiansToDegrees = 57.29577951308232

fun Int.clamp(lower: Int, upper: Int) = min(upper, max(lower, this))

fun Double.clamp(lower: Double, upper: Double) = min(upper, max(lower, this))

fun Int.toRadians() = this * degreesToRadians

fun Double.toRadians() = this * degreesToRadians

fun Double.toDegrees(): Double = this * radiansToDegrees

fun Double.toHeading() =
    (90.0 - this.toDegrees()).let {
        if (it < 0.0) {
            it % 360.0 + 360.0
        } else {
            it % 360.0
        }
    }

fun Double.round(digits: Int) =
    (this * 10.0.pow(digits.absoluteValue)).roundToInt() / 10.0.pow(digits.absoluteValue)

fun Double.format(digits: Int) =
    if (digits < 1) {
        this.toInt().toString()
    } else {
        this.round(digits).toString().let {
            val parts = it.split(".", limit = 2)
            val digitsFound = if (parts.size == 1) 0 else parts[1].length
            if (digitsFound == digits) {
                it
            } else {
                val decimals = if (parts.size == 1) "" else parts[1]
                "${parts[0]}.$decimals${(1..digits - digitsFound).joinToString(separator = "") { "0" }}"
            }
        }
    }

fun Int.pad(width: Int) = toString().padStart(width, '0')

fun Double.oneDigit() = (this * 10).roundToLong() / 10.0

fun Double.twoDigits() = (this * 100).roundToLong() / 100.0

fun Double.fiveDigits() = (this * 100_000).roundToLong() / 100_000.0

fun Vector2.twoDigits() = Vector2(x.twoDigits(), y.twoDigits())

fun Double.toPercent() = (this * 100).roundToInt()

val Int.px
    get() = "${this}px"
