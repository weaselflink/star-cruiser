package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawPill
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement

class CanvasSpinner(
    canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
    private val widthExpr: CanvasDimensions.() -> Double,
    private val heightExpr: CanvasDimensions.() -> Double = { 10.vmin },
    private val decreaseAction: () -> Unit,
    private val decreaseEnabled: () -> Boolean = { true },
    private val increaseAction: () -> Unit,
    private val increaseEnabled: () -> Boolean = { true },
    initialText: String? = null
) : PointerEventHandlerParent() {

    private val ctx = canvas.context2D
    private val CanvasDimensions.buttonWidth
        get() = heightExpr() * 1.2

    private val decreaseButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr() },
        yExpr = { yExpr() },
        widthExpr = { buttonWidth },
        heightExpr = { heightExpr() },
        onClick = { decreaseAction() },
        enabled = { decreaseEnabled() },
        initialText = "◄"
    )
    private val increaseButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr() + (widthExpr() - buttonWidth) },
        yExpr = { yExpr() },
        widthExpr = { buttonWidth },
        heightExpr = { heightExpr() },
        onClick = { increaseAction() },
        enabled = { increaseEnabled() },
        initialText = "►"
    )

    var text: String? = null

    init {
        text = initialText
        addChildren(decreaseButton, increaseButton)
    }

    fun draw() {
        ctx.drawModulation()

        decreaseButton.draw()
        increaseButton.draw()
    }

    private fun CanvasRenderingContext2D.drawModulation() {
        val dim = canvas.dimensions()

        drawBase(dim)
        text?.also {
            drawText(dim, it)
        }
    }

    private fun CanvasRenderingContext2D.drawBase(dim: CanvasDimensions) {
        save()

        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawPill(
            x = dim.xExpr(),
            y = dim.yExpr(),
            width = dim.widthExpr(),
            height = dim.heightExpr()
        )
        fill()

        restore()
    }

    private fun CanvasRenderingContext2D.drawText(dim: CanvasDimensions, string: String) {
        save()

        textStyle(dim)
        val x = dim.xExpr() + dim.widthExpr() * 0.5
        val y = dim.yExpr() - dim.heightExpr() * 0.35
        fillText(string, x, y)

        restore()
    }

    private fun CanvasRenderingContext2D.textStyle(dim: CanvasDimensions) {
        fillStyle = UiStyle.buttonForegroundColor
        textAlign = CanvasTextAlign.CENTER
        textBaseline = CanvasTextBaseline.ALPHABETIC
        val textSize = dim.heightExpr() * 0.5
        font = UiStyle.font(textSize)
    }
}
