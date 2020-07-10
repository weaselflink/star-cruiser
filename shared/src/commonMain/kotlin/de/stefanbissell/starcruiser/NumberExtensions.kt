package de.stefanbissell.starcruiser

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

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
    this.round(digits).toString().let {
        if (it.contains(".")) it else "$it.${(1..digits).joinToString(separator = "") { "0" }}"
    }

fun Int.pad(width: Int) = toString().padStart(width, '0')
