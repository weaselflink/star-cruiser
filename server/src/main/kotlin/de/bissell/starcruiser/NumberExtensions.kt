package de.bissell.starcruiser

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

fun Int.clip(lower: Int, upper: Int) = min(upper, max(lower, this))

fun Double.toRadians() = Math.toRadians(this)

fun Double.toDegrees(): Double = this / PI * 180

fun Double.toHeading() =
    (90.0 - this.toDegrees()).let {
        if (it < 0.0) {
            it + 360.0
        } else {
            it
        }
    }
