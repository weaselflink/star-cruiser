package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandExitShip
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.UiStyle
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import kotlinx.browser.document
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline

class DestroyedUi : PointerEventHandlerParent() {

    var visible = false
    val canvas = document.canvas2d
    val ctx = canvas.context2D

    private val toSelectionButton = CanvasButton(
        canvas = canvas,
        xExpr = { width * 0.5 - 20.vmin },
        yExpr = { height * 0.5 + 15.vmin },
        widthExpr = { 40.vmin },
        onClick = { toSelection() },
        initialText = "To Selection"
    )

    init {
        addChildren(toSelectionButton)
    }

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return visible && super.isInterestedIn(pointerEvent)
    }

    fun draw() {
        ctx.draw()
    }

    private fun CanvasRenderingContext2D.draw() {
        val dim = canvas.dimensions()

        drawText(dim)

        toSelectionButton.draw()
    }

    private fun CanvasRenderingContext2D.drawText(dim: CanvasDimensions) {
        save()

        fillStyle = UiStyle.buttonForegroundColor
        textAlign = CanvasTextAlign.CENTER
        textBaseline = CanvasTextBaseline.ALPHABETIC
        translate(dim.width * 0.5, dim.height * 0.5 - dim.vmax * 5)
        val textSize = dim.vmin * 5
        font = UiStyle.font(textSize)
        fillText("Your ship was destroyed", 0.0, 0.0)

        restore()
    }

    private fun toSelection() {
        ClientSocket.send(CommandExitShip)
    }
}
