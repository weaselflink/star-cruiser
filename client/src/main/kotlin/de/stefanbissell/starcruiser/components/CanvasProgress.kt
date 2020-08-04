package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.context2D
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.MIDDLE
import org.w3c.dom.RIGHT

class CanvasProgress(
    private val canvas: HTMLCanvasElement,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 34 },
    private val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 },
    private val backgroundColorExpr: (Double) -> String = { UiStyle.buttonBackgroundColor },
    private val foregroundColorExpr: (Double) -> String = { UiStyle.buttonForegroundColor }
) {

    private val ctx: CanvasRenderingContext2D = canvas.context2D
    private val backgroundColor
        get() = backgroundColorExpr(progress)
    private val foregroundColor
        get() = foregroundColorExpr(progress)

    var leftText: String? = null
    var centerText: String? = null
    var rightText: String? = null
    var progress: Double = 0.0
        set(value) {
            field = value.clamp(0.0, 1.0)
        }


    fun draw() {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            lineWidth = dim.lineWidth * 0.5
            val textSize = dim.height * 0.7
            font = UiStyle.font(textSize)

            drawBackground(dim)
            drawBorder(dim)
            fillStyle = foregroundColor
            textBaseline = CanvasTextBaseline.MIDDLE
            drawLeftText(dim)
            drawCenterText(dim)
            drawRightText(dim)
            if (progress > 0.0) {
                drawBar(dim, progress)
                clipBar(dim, progress)
                fillStyle = backgroundColor
                drawLeftText(dim)
                drawCenterText(dim)
                drawRightText(dim)
            }

            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawBackground(dim: ComponentDimensions) {
        fillStyle = backgroundColor
        fillRect(dim.bottomX, dim.bottomY, dim.width, -dim.height)
    }

    private fun CanvasRenderingContext2D.drawBorder(dim: ComponentDimensions) {
        strokeStyle = foregroundColor
        strokeRect(dim.bottomX, dim.bottomY, dim.width, -dim.height)
    }

    private fun CanvasRenderingContext2D.drawLeftText(dim: ComponentDimensions) {
        drawText(dim, leftText, dim.bottomX + marginExpr(dim), CanvasTextAlign.LEFT)
    }

    private fun CanvasRenderingContext2D.drawCenterText(dim: ComponentDimensions) {
        drawText(dim, centerText, dim.bottomX + dim.width * 0.5, CanvasTextAlign.CENTER)
    }

    private fun CanvasRenderingContext2D.drawRightText(dim: ComponentDimensions) {
        drawText(dim, rightText, dim.bottomX + dim.width - marginExpr(dim), CanvasTextAlign.RIGHT)
    }

    private fun CanvasRenderingContext2D.drawBar(dim: ComponentDimensions, barWidth: Double) {
        fillStyle = foregroundColor
        drawBarRect(dim, barWidth)
        fill()
    }

    private fun CanvasRenderingContext2D.clipBar(dim: ComponentDimensions, barWidth: Double) {
        drawBarRect(dim, barWidth)
        clip()
    }

    private fun CanvasRenderingContext2D.drawBarRect(dim: ComponentDimensions, barWidth: Double) {
        beginPath()
        rect(dim.bottomX, dim.bottomY, dim.width * barWidth, -dim.height)
    }

    private fun CanvasRenderingContext2D.drawText(
        dim: ComponentDimensions,
        text: String?,
        x: Double,
        align: CanvasTextAlign
    ) {
        if (text != null) {
            textAlign = align
            fillText(text, x, dim.bottomY - dim.height * 0.5)
        }
    }

    private fun marginExpr(dim: ComponentDimensions) = dim.height * 0.2

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}
