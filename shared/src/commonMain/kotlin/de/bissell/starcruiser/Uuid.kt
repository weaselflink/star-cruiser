package de.bissell.starcruiser

import kotlin.random.Random
import kotlin.random.nextUBytes

class Uuid {

    private val bytes: UByteArray

    constructor() { bytes = version4RandomBytes() }

    private constructor(customBytes: UByteArray) { bytes = customBytes }

    override fun toString() =
        bytes.copyOfRange(0, 4).toHex() + "-" +
        bytes.copyOfRange(4, 6).toHex() + "-" +
        bytes.copyOfRange(6, 8).toHex() + "-" +
        bytes.copyOfRange(8, 10).toHex() + "-" +
        bytes.copyOfRange(10, 16).toHex()

    companion object {

        val nil = Uuid(UByteArray(16) { 0u })

        fun version4() = Uuid(version4RandomBytes())

        private fun version4RandomBytes() =
            Random.nextUBytes(16).also {
                it[6] = it[6] and 0x0fu // clear version
                it[6] = it[6] or 0x40u // set to version 4
                it[8] = it[8] and 0x3fu // clear variant
                it[8] = it[8] or 0x80u // set to IETF variant
            }
    }

    private fun UByteArray.toHex() = joinToString(separator = "") {
        it.toString(16).padStart(2, '0')
    }
}
