package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.format
import org.w3c.dom.HTMLCanvasElement

class CapacitorsDisplay(
    canvas: HTMLCanvasElement,
    xExpr: (CanvasDimensions) -> Double = { it.width * 0.5 - it.vmin * 35 },
    yExpr: (CanvasDimensions) -> Double = { it.vmin * 12 },
    widthExpr: (CanvasDimensions) -> Double = { it.vmin * 70 },
    heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    private val canvasProgress = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = widthExpr,
        heightExpr = heightExpr,
        foregroundColorExpr = {
            when {
                it > 0.25 -> UiStyle.buttonForegroundColor
                it > 0.1 -> UiStyle.warningYellow
                else -> UiStyle.warningRed
            }
        }
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
