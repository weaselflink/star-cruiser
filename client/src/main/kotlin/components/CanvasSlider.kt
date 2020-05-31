package components

import CanvasDimensions
import MouseEventHandler
import circle
import de.bissell.starcruiser.clamp
import dimensions
import drawPill
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import px
import kotlin.math.PI

class CanvasSlider(
    private val canvas: HTMLCanvasElement,
    private val ctx: CanvasRenderingContext2D = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double,
    private val heightExpr: (CanvasDimensions) -> Double,
    private val onChange: (Double) -> Unit = {},
    private val lines: List<Double> = emptyList(),
    private val leftText: String? = null,
    private val reverseValue: Boolean = false
) : MouseEventHandler {

    fun draw(value: Double) {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawPill(dim)
            drawText(dim)
            val effectiveValue = if (reverseValue) 1.0 - value else value
            drawKnob(dim, effectiveValue)
            drawLines(dim)

            restore()
        }
    }

    override fun isInterestedIn(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Boolean {
        val dim = currentDimensions(canvas)

        return mouseEvent.offsetX > dim.bottomX && mouseEvent.offsetX < dim.bottomX + dim.width
                && mouseEvent.offsetY > dim.bottomY - dim.height && mouseEvent.offsetY < dim.bottomY
    }

    override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        onChange(clickValue(canvas, mouseEvent))
    }

    override fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        onChange(clickValue(canvas, mouseEvent))
    }

    override fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        onChange(clickValue(canvas, mouseEvent))
    }

    private fun clickValue(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Double {
        val dim = currentDimensions(canvas)

        return if (dim.isHorizontal) {
            (mouseEvent.offsetX - (dim.bottomX + dim.radius)) / (dim.width - dim.radius * 2.0)
        } else {
            -(mouseEvent.offsetY - (dim.bottomY - dim.radius)) / (dim.height - dim.radius * 2.0)
        }.clamp(0.0, 1.0).let {
            if (reverseValue) 1.0 - it else it
        }
    }

    private fun CanvasRenderingContext2D.drawPill(dim: SliderDimensions) {
        lineWidth = dim.lineWidth
        fillStyle = "#111"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        fill()

        strokeStyle = "#888"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawText(dim: SliderDimensions) {
        if (leftText != null) {
            save()

            fillStyle = "#333"
            textAlign = CanvasTextAlign.LEFT
            textBaseline = CanvasTextBaseline.ALPHABETIC
            translate(dim.bottomX, dim.bottomY)
            if (dim.isHorizontal) {
                val textSize = (dim.height * 0.5).toInt()
                font = "${textSize.px} sans-serif"
                translate(dim.height * 0.4, -dim.height * 0.35)
                fillText(leftText, 0.0, 0.0, dim.width - dim.height)
            } else {
                val textSize = (dim.width * 0.5).toInt()
                font = "${textSize.px} sans-serif"
                translate(dim.width * 0.65, -dim.width * 0.4)
                rotate(-PI * 0.5)
                fillText(leftText, 0.0, 0.0, dim.height - dim.width)
            }

            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawKnob(dim: SliderDimensions, value: Double) {
        fillStyle = "#999"
        beginPath()
        if (dim.isHorizontal) {
            circle(
                dim.bottomX + dim.radius + value.clamp(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                dim.bottomY - dim.radius,
                dim.radius * 0.8
            )
        } else {
            circle(
                dim.bottomX + dim.radius,
                dim.bottomY - dim.radius - value.clamp(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                dim.radius * 0.8
            )
        }
        fill()
    }

    private fun CanvasRenderingContext2D.drawLines(dim: SliderDimensions) {
        strokeStyle = "#666"
        lines.forEach {
            beginPath()
            if (dim.isHorizontal) {
                moveTo(dim.bottomX + dim.radius + it * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 0.4)
                lineTo(dim.bottomX + dim.radius + it * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 1.6)
            } else {
                moveTo(dim.bottomX + dim.radius * 0.4, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
                lineTo(dim.bottomX + dim.radius * 1.6, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
            }
            stroke()
        }
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        canvas.dimensions().let { dim ->
            val width = widthExpr(dim)
            val height = heightExpr(dim)
            SliderDimensions(
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

private data class SliderDimensions(
    val bottomX: Double,
    val bottomY: Double,
    val width: Double,
    val height: Double,
    val radius: Double,
    val length: Double,
    val lineWidth: Double,
    val isHorizontal: Boolean = width > height
)
