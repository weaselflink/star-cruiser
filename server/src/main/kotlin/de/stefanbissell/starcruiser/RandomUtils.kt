package de.stefanbissell.starcruiser

import kotlin.random.Random

fun randomShipName(): String {
    return "${randomLetter()}${randomLetter()}${randomPaddedNumber()}"
}

private fun randomPaddedNumber(n: Int = 3) = (0 until n)
    .joinToString(separator = "") { Random.nextInt(10).toString() }

fun randomLetter(): Char {
    return ('A'.toInt() + Random.nextInt(26)).toChar()
}

fun randomAngle() = Random.nextDouble(fullCircle)
