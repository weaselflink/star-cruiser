package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.ScanProgressMessage
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.math.abs
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

    private val canvasPopup = CanvasPopup(canvas)
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

        val title = "Scanning ${scanProgress.designation}"
        canvasPopup.draw(dim, title)

        save()

        drawNoise(dim, scanProgress.noise)

        restore()

        scanProgress.input.forEachIndexed { index, value ->
            inputs[index].draw(value)
        }

        abortButton.draw()
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

        val amplitude = height * 0.4
        val noiseWave = createNoiseWave(noise)

        save()

        lineWidth = 0.4.vmin

        strokeStyle = UiStyle.backgroundColor
        beginPath()
        moveTo(x, middle)
        lineTo(x + width, middle)
        stroke()

        strokeStyle = UiStyle.buttonForegroundColor

        strokeRect(x, y, width, height)
        beginPath()
        rect(x, y, width, height)
        clip()

        beginPath()
        moveTo(x, middle)
        noiseWave.forEach {
            lineTo(
                x + width / noiseWave.size.toDouble() * it.first,
                it.second * amplitude + middle
            )
        }
        lineTo(x + width, middle)
        stroke()

        restore()
    }

    private fun createNoiseWave(noise: Double): List<Pair<Int, Double>> {
        val points = 200
        val noiseFunctions: List<(Int) -> Double> = (0..6).map {
            createNoiseFunction(points, noise)
        }
        val finalFunction: (Int) -> Double = { index ->
            val pos = index / points.toDouble() * 6.0 * PI
            sin(pos) * -1.0 + noiseFunctions.map { it(index) }.sum()
        }
        val noiseValues = (1 until points).map {
            it to finalFunction(it)
        }
        val maxNoise = noiseValues.map { abs(it.second) }.maxOrNull() ?: 1.0
        return noiseValues.map { it.first to it.second / maxNoise }
    }

    private fun createNoiseFunction(points: Int, noise: Double): (Int) -> Double {
        val frequency = (Random.nextDouble() * 24.0 + 12.0) * PI
        val offset = Random.nextDouble() * 2.0 * PI
        return { sin(it / points.toDouble() * frequency + offset) * noise }
    }

    private val Int.vmin
        get() = canvas.dimensions().vmin * this

    private val Double.vmin
        get() = canvas.dimensions().vmin * this
}
