package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.ScanProgressMessage
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.send
import org.w3c.dom.BOTTOM
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement

class ScanDisplay(
    val canvas: HTMLCanvasElement,
    val xExpr: (CanvasDimensions) -> Double = { it.width * 0.5 - it.vmin * 42 },
    val yExpr: (CanvasDimensions) -> Double = { it.height * 0.5 + it.vmin * 30 },
    val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 84 },
    val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 60 }
) : PointerEventHandler {

    private val ctx = canvas.context2D
    private var visible = false

    private val abortButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr(it) + widthExpr(it) - it.vmin * 25 },
        yExpr = { yExpr(it) - it.vmin * 5 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { clientSocket.send(Command.CommandAbortScan) },
        initialText = "Abort"
    )

    override fun isInterestedIn(pointerEvent: PointerEvent) = visible

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        if (abortButton.isInterestedIn(pointerEvent)) {
            abortButton.handlePointerDown(pointerEvent)
        }
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        if (abortButton.isInterestedIn(pointerEvent)) {
            abortButton.handlePointerUp(pointerEvent)
        }
    }

    fun draw(scanProgress: ScanProgressMessage?) {
        visible = scanProgress != null

        if (scanProgress != null) {
            ctx.draw(scanProgress)
        }
    }

    private fun CanvasRenderingContext2D.draw(scanProgress: ScanProgressMessage) {
        val dim = ComponentDimensions.calculateRect(canvas, xExpr, yExpr, widthExpr, heightExpr)

        save()

        fillStyle = UiStyle.buttonBackgroundColor
        lineWidth = UiStyle.buttonLineWidth.vmin
        beginPath()
        drawRect(dim)
        fill()

        strokeStyle = UiStyle.buttonForegroundColor
        beginPath()
        drawRect(dim)
        stroke()

        drawHeader(dim, scanProgress.designation)

        restore()

        abortButton.draw()
    }

    private fun CanvasRenderingContext2D.drawHeader(
        dim: ComponentDimensions,
        designation: String
    ) {
        val x = dim.bottomX + 5.vmin
        val y = dim.bottomY - dim.height + 10.vmin

        val text = "Scanning $designation"

        save()

        font = UiStyle.font(5.vmin)
        textBaseline = CanvasTextBaseline.BOTTOM
        fillStyle = UiStyle.buttonForegroundColor
        fillText(text, x, y)

        restore()
    }

    private val Int.vmin
        get() = canvas.dimensions().vmin * this

    private val Double.vmin
        get() = canvas.dimensions().vmin * this
}
