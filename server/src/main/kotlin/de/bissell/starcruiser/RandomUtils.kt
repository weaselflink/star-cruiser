package de.bissell.starcruiser

import kotlin.random.Random

fun randomShipName(): String {
    return randomLetter().toString() + randomLetter().toString() + "-" + Random.nextInt(10_000).toString()
}

fun randomLetter(): Char {
    return ('A'.toInt() + Random.nextInt(26)).toChar()
}
