package components

import CanvasDimensions
import circle
import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.clamp
import drawPill
import input.PointerEvent
import input.PointerEventHandler
import org.w3c.dom.*
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
) : PointerEventHandler {

    fun draw(value: Double) {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawPill(dim)
            drawText(dim)
            val effectiveValue = if (reverseValue) 1.0 - value else value
            drawKnob(dim, effectiveValue)
            drawKnobText(dim, effectiveValue)
            drawLines(dim)

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
        onChange(clickValue(pointerEvent.point))
    }

    override fun handlePointerMove(pointerEvent: PointerEvent) {
        onChange(clickValue(pointerEvent.point))
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        onChange(clickValue(pointerEvent.point))
    }

    private fun clickValue(point: Vector2): Double {
        val dim = currentDimensions(canvas)

        return if (dim.isHorizontal) {
            (point.x - (dim.bottomX + dim.radius)) / (dim.width - dim.radius * 2.0)
        } else {
            -(point.y - (dim.bottomY - dim.radius)) / (dim.height - dim.radius * 2.0)
        }.clamp(0.0, 1.0).let {
            if (reverseValue) 1.0 - it else it
        }
    }

    private fun CanvasRenderingContext2D.drawPill(dim: ComponentDimensions) {
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

    private fun CanvasRenderingContext2D.drawText(dim: ComponentDimensions) {
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

    private fun CanvasRenderingContext2D.drawKnob(dim: ComponentDimensions, value: Double) {
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

    private fun CanvasRenderingContext2D.drawKnobText(dim: ComponentDimensions, value: Double) {
        if (leftText != null) {
            save()

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
            clip()

            fillStyle = "#fff"
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

    private fun CanvasRenderingContext2D.drawLines(dim: ComponentDimensions) {
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
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}
