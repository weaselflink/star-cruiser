package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.drawPill
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement

class CanvasButton(
    private val canvas: HTMLCanvasElement,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double,
    private val heightExpr: (CanvasDimensions) -> Double,
    private val onClick: () -> Unit = {},
    private val activated: () -> Boolean = { false },
    private val text: () -> String? = { null }
) : PointerEventHandler {

    private val ctx: CanvasRenderingContext2D = canvas.context2D

    private var pressed = false

    fun draw() {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawPill(dim)
            drawText(dim)

            restore()
        }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent) =
        currentDimensions(canvas).isInside(pointerEvent)

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        pressed = true
        onClick()
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        pressed = false
    }

    private fun CanvasRenderingContext2D.drawPill(dim: ComponentDimensions) {
        lineWidth = dim.lineWidth
        fillStyle = if (pressed) {
            UiStyle.buttonPressedColor
        } else {
            if (activated()) {
                UiStyle.buttonForegroundColor
            } else {
                UiStyle.buttonBackgroundColor
            }
        }
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        fill()

        strokeStyle = UiStyle.buttonForegroundColor
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawText(dim: ComponentDimensions) {
        val currentText = text()
        if (currentText != null) {
            save()

            fillStyle = if (activated()) {
                UiStyle.buttonBackgroundColor
            } else {
                UiStyle.buttonForegroundColor
            }
            textAlign = CanvasTextAlign.CENTER
            textBaseline = CanvasTextBaseline.ALPHABETIC
            translate(dim.bottomX, dim.bottomY)
            val textSize = (dim.height * 0.5).toInt()
            font = UiStyle.font(textSize)
            translate(dim.width * 0.5, -dim.height * 0.35)
            fillText(currentText, 0.0, 0.0)
        }
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}
