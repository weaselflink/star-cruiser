package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ShipMessage
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

class HullDisplay(
    canvas: HTMLCanvasElement,
    xExpr: (CanvasDimensions) -> Double,
    yExpr: (CanvasDimensions) -> Double,
    widthExpr: (CanvasDimensions) -> Double = { it.vmin * 40 },
    heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    private val canvasProgress = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = widthExpr,
        heightExpr = heightExpr,
        backgroundColor = "#111",
        foregroundColor = "#888"
    )

    init {
        canvasProgress.leftText = "Hull"
    }

    fun draw(shipMessage: ShipMessage) {
        val progress = shipMessage.hull / shipMessage.hullMax
        canvasProgress.progress = progress
        canvasProgress.rightText = "${(progress * 100).roundToInt()}%"

        canvasProgress.draw()
    }
}
