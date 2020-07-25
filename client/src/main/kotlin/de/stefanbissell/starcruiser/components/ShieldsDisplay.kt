package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement

class ShieldsDisplay(
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
        heightExpr = heightExpr
    )

    fun draw(shieldMessage: ShieldMessage) {
        with(shieldMessage) {
            canvasProgress.leftText = if (up) {
                "Shields up"
            } else {
                "Shields down"
            }
            val progress = strength / max
            canvasProgress.progress = progress
            canvasProgress.rightText = "${progress.toPercent()}%"
        }

        canvasProgress.draw()
    }
}
