package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.StationUi
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

abstract class CanvasUi(
    override val station: Station
) : StationUi() {

    val canvas: HTMLCanvasElement = document.body!!.canvas
    val ctx = canvas.context2D
}
