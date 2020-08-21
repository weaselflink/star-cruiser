package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.ScanProgressMessage
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.send
import org.w3c.dom.BOTTOM
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ScanDisplay(
    val canvas: HTMLCanvasElement,
    val xExpr: (CanvasDimensions) -> Double = { it.width * 0.5 - it.vmin * 42 },
    var yExpr: (CanvasDimensions) -> Double = { it.height * 0.5 + it.vmin * 30 },
    val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 84 },
    var heightExpr: (CanvasDimensions) -> Double = { it.vmin * 60 }
) : PointerEventHandlerParent() {

    private val ctx = canvas.context2D
    private var visible = false

    private val inputs = mutableListOf<CanvasSlider>()
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

    init {
        addChildren(abortButton)
    }

    fun draw(scanProgress: ScanProgressMessage?) {
        visible = scanProgress != null

        if (scanProgress != null) {
            updateInputs(scanProgress)

            ctx.draw(scanProgress)
        }
    }

    private fun updateInputs(scanProgress: ScanProgressMessage) {
        yExpr = { it.height * 0.5 + it.vmin * (scanProgress.input.size * 12 + 50) * 0.5 }
        heightExpr = { it.vmin * (scanProgress.input.size * 12 + 50) }

        if (inputs.size > scanProgress.input.size) {
            repeat(inputs.size - scanProgress.input.size) {
                val removed = inputs.removeAt(inputs.size - 1)
                removeChildren(removed)
            }
        }
        if (inputs.size < scanProgress.input.size) {
            (inputs.size until scanProgress.input.size).forEach { index ->
                val slider = CanvasSlider(
                    canvas = canvas,
                    xExpr = { xExpr(it) + 5.vmin },
                    yExpr = { yExpr(it) - heightExpr(it) + it.vmin * 45 + it.vmin * 12 * index },
                    widthExpr = { widthExpr(it) - it.vmin * 10 },
                    heightExpr = { it.vmin * 10 },
                    onChange = {
                        clientSocket.send(Command.CommandSolveScanGame(index, it))
                    }
                )
                inputs += slider
                addChildren(slider)
            }
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

        drawNoise(dim, scanProgress.noise)

        restore()

        scanProgress.input.forEachIndexed { index, value ->
            inputs[index].draw(value)
        }

        abortButton.draw()
    }

    private fun CanvasRenderingContext2D.drawHeader(
        dim: ComponentDimensions,
        designation: String
    ) {
        val x = dim.bottomX + 5.vmin
        val y = dim.bottomY - dim.height + 8.vmin

        val text = "Scanning $designation"

        save()

        font = UiStyle.font(5.vmin)
        textBaseline = CanvasTextBaseline.BOTTOM
        fillStyle = UiStyle.buttonForegroundColor
        fillText(text, x, y)

        restore()
    }

    private fun CanvasRenderingContext2D.drawNoise(
        dim: ComponentDimensions,
        noise: Double
    ) {
        val x = dim.bottomX + 5.vmin
        val y = dim.bottomY - dim.height + 11.vmin
        val middle = dim.bottomY - dim.height + 20.vmin
        val width = dim.width - 10.vmin
        val height = 18.vmin

        val noiseFunction: (Int) -> Double = {
            val pos = it / 100.0 * PI * 6.0
            val amplitude = height * 0.8
            sin(pos) * amplitude * -0.5 + middle + (Random.nextDouble() - 0.5) * amplitude * noise
        }

        save()

        strokeStyle = UiStyle.buttonForegroundColor

        strokeRect(x, y, width, height)
        beginPath()
        rect(x, y, width, height)
        clip()

        beginPath()
        moveTo(x, middle)
        (1..99).forEach { index ->
            lineTo(x + width * 0.01 * index, noiseFunction(index))
        }
        lineTo(x + width, middle)
        stroke()

        restore()
    }

    private val Int.vmin
        get() = canvas.dimensions().vmin * this

    private val Double.vmin
        get() = canvas.dimensions().vmin * this
}
