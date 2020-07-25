package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.px

object UiStyle {

    const val backgroundColor = "#222"
    const val mapBackgroundColor = "#000"
    const val scopeBackgroundColor = "#000"

    const val buttonBackgroundColor = "#111"
    const val buttonForegroundColor = "#888"
    const val buttonPressedColor = "#333"
    const val buttonLineWidth = 0.4

    const val fontFamily = "sans-serif"

    fun font(size: Number) = "${size.toInt().px} $fontFamily"
    fun boldFont(size: Number) = "bold ${size.toInt().px} $fontFamily"
}
