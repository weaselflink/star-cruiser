package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.format
import org.w3c.dom.HTMLCanvasElement

class CapacitorsDisplay(
    canvas: HTMLCanvasElement,
    xExpr: CanvasDimensions.() -> Double = { width * 0.5 - vmin * 40 },
    yExpr: CanvasDimensions.() -> Double = { height - vmin * 6 - vmin * 10 * PoweredSystemType.values().size },
    widthExpr: CanvasDimensions.() -> Double = { vmin * 80 },
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
        canvasProgress.leftText = "Capacitors"
    }

    fun draw(powerSettings: PowerMessage) {
        with(powerSettings) {
            val prediction = powerSettings.capacitorsPrediction
            canvasProgress.leftText = when {
                prediction == null -> "Capacitors"
                prediction > 60 -> "Capacitors (full in >60s)"
                prediction > 0 -> "Capacitors (full in ${prediction}s)"
                prediction < -60 -> "Capacitors (empty in >60s)"
                prediction < 0 -> "Capacitors (empty in ${-prediction}s)"
                else -> "Capacitors"
            }
            canvasProgress.rightText = capacitors.format(1)
            canvasProgress.progress = capacitors / maxCapacitors
        }

        canvasProgress.draw()
    }
}
