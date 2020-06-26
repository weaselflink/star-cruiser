package components

import CanvasDimensions
import dimensions
import drawPill
import input.PointerEvent
import input.PointerEventHandler
import org.w3c.dom.*
import px

class CanvasButton(
    private val canvas: HTMLCanvasElement,
    private val ctx: CanvasRenderingContext2D = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double,
    private val heightExpr: (CanvasDimensions) -> Double,
    private val onClick: () -> Unit = {},
    private val text: String? = null
) : PointerEventHandler {

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

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        val dim = currentDimensions(canvas)
        val point = pointerEvent.point

        return point.x > dim.bottomX && point.x < dim.bottomX + dim.width
                && point.y > dim.bottomY - dim.height && point.y < dim.bottomY
    }

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        pressed = true
        onClick()
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        pressed = false
    }

    private fun CanvasRenderingContext2D.drawPill(dim: ButtonDimensions) {
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

    private fun CanvasRenderingContext2D.drawText(dim: ButtonDimensions) {
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
        canvas.dimensions().let { dim ->
            val width = widthExpr(dim)
            val height = heightExpr(dim)
            ButtonDimensions(
                bottomX = xExpr(dim),
                bottomY = yExpr(dim),
                width = width,
                height = height,
                radius = if (width > height) height * 0.5 else width * 0.5,
                length = if (width > height) width else height,
                lineWidth = dim.vmin * 0.4
            )
        }
}

private data class ButtonDimensions(
    val bottomX: Double,
    val bottomY: Double,
    val width: Double,
    val height: Double,
    val radius: Double,
    val length: Double,
    val lineWidth: Double
)
