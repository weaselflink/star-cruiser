package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement

class HullDisplay(
    canvas: HTMLCanvasElement,
    xExpr: CanvasDimensions.() -> Double,
    yExpr: CanvasDimensions.() -> Double,
    widthExpr: CanvasDimensions.() -> Double = { vmin * 46 },
    heightExpr: CanvasDimensions.() -> Double = { vmin * 6 }
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
        canvasProgress.leftText = "Hull"
    }

    fun draw(ratio: Double) {
        canvasProgress.progress = ratio
        canvasProgress.rightText = "${ratio.toPercent()}%"

        canvasProgress.draw()
    }
}
