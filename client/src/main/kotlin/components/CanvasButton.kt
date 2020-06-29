package components

import CanvasDimensions
import context2D
import drawPill
import input.PointerEvent
import input.PointerEventHandler
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import px

class CanvasButton(
    private val canvas: HTMLCanvasElement,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double,
    private val heightExpr: (CanvasDimensions) -> Double,
    private val onClick: () -> Unit = {},
    private val text: String? = null
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
        fillStyle = if (pressed) "#333" else "#111"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        fill()

        strokeStyle = "#888"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawText(dim: ComponentDimensions) {
        if (text != null) {
            save()

            fillStyle = "#888"
            textAlign = CanvasTextAlign.CENTER
            textBaseline = CanvasTextBaseline.ALPHABETIC
            translate(dim.bottomX, dim.bottomY)
            val textSize = (dim.height * 0.5).toInt()
            font = "${textSize.px} sans-serif"
            translate(dim.width * 0.5, -dim.height * 0.35)
            fillText(text, 0.0, 0.0)
        }
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}

