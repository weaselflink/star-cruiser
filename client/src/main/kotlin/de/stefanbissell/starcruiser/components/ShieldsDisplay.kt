package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.SimpleShieldMessage
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement

class ShieldsDisplay(
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
            if (isUp) {
                when {
                    it > 0.25 -> UiStyle.buttonForegroundColor
                    it > 0.1 -> UiStyle.warningYellow
                    else -> UiStyle.warningRed
                }
            } else {
                when {
                    it > 0.25 -> UiStyle.buttonPressedColor
                    it > 0.1 -> UiStyle.warningYellowDark
                    else -> UiStyle.warningRedDark
                }
            }
        }
    )
    private var isUp = true

    fun draw(shieldMessage: ShieldMessage) {
        with(shieldMessage) {
            isUp = up
            canvasProgress.leftText = if (isUp) {
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

    fun draw(shieldMessage: SimpleShieldMessage) {
        with(shieldMessage) {
            isUp = up
            canvasProgress.leftText = if (isUp) {
                "Shields up"
            } else {
                "Shields down"
            }
            canvasProgress.progress = ratio
            canvasProgress.rightText = "${ratio.toPercent()}%"
        }

        canvasProgress.draw()
    }
}
