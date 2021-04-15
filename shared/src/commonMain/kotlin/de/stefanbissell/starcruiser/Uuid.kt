package de.stefanbissell.starcruiser

import kotlin.random.Random
import kotlin.random.nextUBytes

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
data class Uuid(
    private val bytes: UByteArray = version4RandomBytes()
) {

    override fun toString() =
        bytes.copyOfRange(0, 4).toHex() + "-" +
            bytes.copyOfRange(4, 6).toHex() + "-" +
            bytes.copyOfRange(6, 8).toHex() + "-" +
            bytes.copyOfRange(8, 10).toHex() + "-" +
            bytes.copyOfRange(10, 16).toHex()

    companion object {
        private fun version4RandomBytes() =
            Random.nextUBytes(16).also {
                it[6] = it[6] and 0x0fU // clear version
                it[6] = it[6] or 0x40U // set to version 4
                it[8] = it[8] and 0x3fU // clear variant
                it[8] = it[8] or 0x80U // set to IETF variant
            }
    }

    private fun UByteArray.toHex() = joinToString(separator = "") {
        it.toString(16).padStart(2, '0')
    }
}
