package components

import CanvasDimensions
import de.stefanbissell.starcruiser.ShieldMessage
import kotlin.math.roundToInt
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
        heightExpr = heightExpr,
        backgroundColor = "#111",
        foregroundColor = "#888"
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
            canvasProgress.rightText = "${(progress * 100).roundToInt()}%"
        }

        canvasProgress.draw()
    }
}
