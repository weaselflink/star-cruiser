package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.format
import org.w3c.dom.HTMLCanvasElement

class CapacitorsDisplay(
    canvas: HTMLCanvasElement,
    xExpr: (CanvasDimensions) -> Double,
    yExpr: (CanvasDimensions) -> Double,
    widthExpr: (CanvasDimensions) -> Double = { it.vmin * 60 },
    heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    private val canvasProgress = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = widthExpr,
        heightExpr = heightExpr
    )

    init {
        canvasProgress.leftText = "Capacitors"
    }

    fun draw(powerSettings: PowerMessage) {
        with(powerSettings) {
            canvasProgress.rightText = capacitors.format(1)
            canvasProgress.progress = capacitors / maxCapacitors
        }

        canvasProgress.draw()
    }
}
