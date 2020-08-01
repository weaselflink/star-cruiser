package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.PowerMessage
import org.w3c.dom.HTMLCanvasElement

class RepairDisplay(
    canvas: HTMLCanvasElement,
    xExpr: (CanvasDimensions) -> Double = { it.width * 0.5 - it.vmin * 35 },
    yExpr: (CanvasDimensions) -> Double = { it.vmin * 20 },
    widthExpr: (CanvasDimensions) -> Double = { it.vmin * 70 },
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
        val repairProgress = powerSettings.settings.values
            .mapNotNull { it.repairProgress }
            .firstOrNull()

        if (repairProgress != null) {
            canvasProgress.rightText = "${repairProgress.remainingTime}s"
            canvasProgress.progress = repairProgress.progress

            canvasProgress.draw()
        }
    }
}
