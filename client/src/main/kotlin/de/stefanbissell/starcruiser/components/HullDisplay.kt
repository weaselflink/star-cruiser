package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement

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

    fun draw(snapshot: SnapshotMessage.Weapons) {
        val progress = snapshot.hull / snapshot.hullMax
        canvasProgress.progress = progress
        canvasProgress.rightText = "${progress.toPercent()}%"

        canvasProgress.draw()
    }
}
