package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.circle
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.drawPill
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import kotlin.math.PI

class CanvasSlider(
    private val canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
    private val widthExpr: CanvasDimensions.() -> Double,
    private val heightExpr: CanvasDimensions.() -> Double = { 10.vmin },
    private val onChange: (Double) -> Unit = {},
    private val lines: List<Double> = emptyList(),
    private val leftText: String? = null,
    private val reverseValue: Boolean = false
) : PointerEventHandler {

    private val ctx: CanvasRenderingContext2D = canvas.context2D

    fun draw(value: Double) {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawBase(dim)
            drawBackgroundText(dim)
            val effectiveValue = if (reverseValue) 1.0 - value else value
            drawKnob(dim, effectiveValue)
            drawKnobText(dim, effectiveValue)
            drawLines(dim)

            restore()
        }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent) =
        currentDimensions(canvas).isInside(pointerEvent)

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
            (point.x - (dim.leftX + dim.radius)) / (dim.width - dim.radius * 2.0)
        } else {
            -(point.y - (dim.bottomY - dim.radius)) / (dim.height - dim.radius * 2.0)
        }.clamp(0.0, 1.0).let {
            if (reverseValue) 1.0 - it else it
        }
    }

    private fun CanvasRenderingContext2D.drawBase(dim: ComponentDimensions) {
        lineWidth = dim.lineWidth
        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawPill(dim)
        fill()

        strokeStyle = UiStyle.buttonForegroundColor
        beginPath()
        drawPill(dim)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawBackgroundText(dim: ComponentDimensions) {
        if (leftText != null) {
            save()

            fillStyle = "#333"
            drawText(dim, leftText)

            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawKnob(dim: ComponentDimensions, value: Double) {
        fillStyle = "#999"
        drawKnobPath(dim, value)
        fill()
    }

    private fun CanvasRenderingContext2D.drawKnobText(dim: ComponentDimensions, value: Double) {
        if (leftText != null) {
            save()

            drawKnobPath(dim, value)
            clip()

            fillStyle = "#fff"
            drawText(dim, leftText)

            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawText(
        dim: ComponentDimensions,
        text: String
    ) {
        textAlign = CanvasTextAlign.LEFT
        textBaseline = CanvasTextBaseline.ALPHABETIC
        translate(dim.leftX, dim.bottomY)
        if (dim.isHorizontal) {
            val textSize = dim.height * 0.5
            font = UiStyle.font(textSize)
            translate(dim.height * 0.4, -dim.height * 0.35)
            fillText(text, 0.0, 0.0, dim.width - dim.height)
        } else {
            val textSize = dim.width * 0.5
            font = UiStyle.font(textSize)
            translate(dim.width * 0.65, -dim.width * 0.4)
            rotate(-PI * 0.5)
            fillText(text, 0.0, 0.0, dim.height - dim.width)
        }
    }

    private fun CanvasRenderingContext2D.drawKnobPath(
        dim: ComponentDimensions,
        value: Double
    ) {
        beginPath()
        if (dim.isHorizontal) {
            circle(
                dim.leftX + dim.radius + value.clamp(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                dim.bottomY - dim.radius,
                dim.radius * 0.8
            )
        } else {
            circle(
                dim.leftX + dim.radius,
                dim.bottomY - dim.radius - value.clamp(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                dim.radius * 0.8
            )
        }
    }

    private fun CanvasRenderingContext2D.drawLines(dim: ComponentDimensions) {
        strokeStyle = "#666"
        lines.forEach {
            beginPath()
            if (dim.isHorizontal) {
                moveTo(dim.leftX + dim.radius + it * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 0.4)
                lineTo(dim.leftX + dim.radius + it * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 1.6)
            } else {
                moveTo(dim.leftX + dim.radius * 0.4, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
                lineTo(dim.leftX + dim.radius * 1.6, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
            }
            stroke()
        }
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculatePill(
            canvas = canvas,
            xExpr = xExpr,
            yExpr = yExpr,
            widthExpr = widthExpr,
            heightExpr = heightExpr
        )
}
