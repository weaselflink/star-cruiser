package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement

class RepairDisplay(
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
        canvasProgress.leftText = "Repairing"
    }

    fun draw(powerSettings: PowerMessage) {
        val progress = powerSettings.settings.values
            .mapNotNull { it.repairProgress }
            .firstOrNull()

        if (progress != null) {
            canvasProgress.rightText = "${progress.toPercent()}%"
            canvasProgress.progress = progress

            canvasProgress.draw()
        }
    }
}
