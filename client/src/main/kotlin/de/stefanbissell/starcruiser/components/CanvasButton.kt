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
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
    private val widthExpr: CanvasDimensions.() -> Double,
    private val heightExpr: CanvasDimensions.() -> Double = { 10.vmin },
    private val onClick: () -> Unit = {},
    private val activated: () -> Boolean = { false },
    private val enabled: () -> Boolean = { true },
    initialText: String? = null
) : PointerEventHandler {

    private val ctx: CanvasRenderingContext2D = canvas.context2D

    private var pressed = false
    var text: String? = null

    init {
        text = initialText
    }

    fun draw() {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawBase(dim)
            drawText(dim)

            restore()
        }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent) =
        enabled() && currentDimensions(canvas).isInside(pointerEvent)

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        pressed = true
        onClick()
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        pressed = false
    }

    private fun CanvasRenderingContext2D.drawBase(dim: ComponentDimensions) {
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
        drawPill(dim)
        fill()

        strokeStyle = if (enabled()) {
            UiStyle.buttonForegroundColor
        } else {
            UiStyle.buttonPressedColor
        }
        beginPath()
        drawPill(dim)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawText(dim: ComponentDimensions) {
        text?.also {
            fillStyle = when {
                activated() -> UiStyle.buttonBackgroundColor
                enabled() -> UiStyle.buttonForegroundColor
                else -> UiStyle.buttonPressedColor
            }
            textAlign = CanvasTextAlign.CENTER
            textBaseline = CanvasTextBaseline.ALPHABETIC
            translate(dim.leftX, dim.bottomY)
            val textSize = dim.height * 0.5
            font = UiStyle.font(textSize)
            translate(dim.width * 0.5, -dim.height * 0.35)
            fillText(it, 0.0, 0.0)
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
