package de.stefanbissell.starcruiser

import kotlin.math.PI

fun smallestSignedAngleBetween(source: Number, target: Number): Double {
    val x = source.toDouble()
    val y = target.toDouble()
    val a = (y - x)
    return if (a >= -PI && a <= PI) {
        a
    } else {
        if (a > PI) {
            -(2 * PI - a)
        } else {
            a + 2 * PI
        }
    }
}
