package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.px

object UiStyle {

    private val yellow = HslColor(51)
    private val red = HslColor(16)

    const val backgroundColor = "#222"
    const val mapBackgroundColor = "#000"
    const val scopeBackgroundColor = "#000"

    const val buttonBackgroundColor = "#111"
    const val buttonForegroundColor = "#888"
    const val buttonPressedColor = "#333"
    const val buttonLineWidth = 0.4

    val warningYellow = yellow.toString()
    val warningRed = red.toString()
    val warningYellowDark = yellow.copy(lightness = 30).toString()
    val warningRedDark = red.copy(lightness = 30).toString()

    private const val fontFamily = "sans-serif"

    fun font(size: Number) = "${size.toInt().px} $fontFamily"
    fun boldFont(size: Number) = "bold ${size.toInt().px} $fontFamily"
}

private data class HslColor(
    val hue: Int,
    val saturation: Int = 100,
    val lightness: Int = 50
) {

    override fun toString() = "hsl($hue, $saturation%, $lightness%)"
}
