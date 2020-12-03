package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ClientSocket
import de.stefanbissell.starcruiser.Command
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

class ModulationUi(
    canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
    private val widthExpr: CanvasDimensions.() -> Double = { 46.vmin },
    private val heightExpr: CanvasDimensions.() -> Double = { 10.vmin },
    private val decreaseCommand: Command,
    private val increaseCommand: Command
) : PointerEventHandlerParent() {

    private val ctx = canvas.context2D
    private val CanvasDimensions.buttonWidth
        get() = 12.vmin

    private val decreaseButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr() },
        yExpr = { yExpr() },
        widthExpr = { buttonWidth },
        heightExpr = { heightExpr() },
        onClick = { ClientSocket.send(decreaseCommand) },
        initialText = "◄"
    )
    private val increaseButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr() + (widthExpr() - buttonWidth) },
        yExpr = { yExpr() },
        widthExpr = { buttonWidth },
        heightExpr = { heightExpr() },
        onClick = { ClientSocket.send(increaseCommand) },
        initialText = "►"
    )

    init {
        addChildren(decreaseButton, increaseButton)
    }

    fun draw(modulation: Int) {
        ctx.drawModulation(modulation)

        decreaseButton.draw()
        increaseButton.draw()
    }

    private fun CanvasRenderingContext2D.drawModulation(modulation: Int) {
        val dim = canvas.dimensions()

        drawBase(dim)
        drawText(dim, modulation)
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

    private fun CanvasRenderingContext2D.drawText(dim: CanvasDimensions, modulation: Int) {
        save()

        modulationTextStyle(dim)
        val x = dim.xExpr() + dim.widthExpr() * 0.5
        val y = dim.yExpr() - dim.heightExpr() * 0.35
        val value = modulation * 2 + 78
        val text = "$value PHz"
        fillText(text, x, y)

        restore()
    }

    private fun CanvasRenderingContext2D.modulationTextStyle(dim: CanvasDimensions) {
        fillStyle = UiStyle.buttonForegroundColor
        textAlign = CanvasTextAlign.CENTER
        textBaseline = CanvasTextBaseline.ALPHABETIC
        val textSize = dim.heightExpr() * 0.5
        font = UiStyle.font(textSize)
    }
}
