package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.RepairProgressMessage
import de.stefanbissell.starcruiser.circle
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.send
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND

class RepairDisplay(
    val canvas: HTMLCanvasElement,
    val xExpr: (CanvasDimensions) -> Double = { it.width * 0.5 - it.vmin * 42 },
    val yExpr: (CanvasDimensions) -> Double = { it.height * 0.5 + it.vmin * 30 },
    val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 84 },
    val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 60 }
) : PointerEventHandler {

    private val ctx = canvas.context2D
    private var visible = false
    private val tiles = mutableListOf<Tile>()

    private val canvasPopup = CanvasPopup(canvas)
    private val abortButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr(it) + widthExpr(it) - it.vmin * 25 },
        yExpr = { yExpr(it) - it.vmin * 5 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { clientSocket.send(Command.CommandAbortRepair) },
        initialText = "Abort"
    )

    override fun isInterestedIn(pointerEvent: PointerEvent) = visible

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        tiles.firstOrNull {
            it.isInterestedIn(pointerEvent)
        }?.click()

        if (abortButton.isInterestedIn(pointerEvent)) {
            abortButton.handlePointerDown(pointerEvent)
        }
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        if (abortButton.isInterestedIn(pointerEvent)) {
            abortButton.handlePointerUp(pointerEvent)
        }
    }

    fun draw(powerSettings: PowerMessage) {
        visible = powerSettings.repairProgress != null

        powerSettings.repairProgress?.also {
            parseTiles(it)
            ctx.draw(it)
        }
    }

    private fun CanvasRenderingContext2D.draw(repairProgress: RepairProgressMessage) {
        val dim = ComponentDimensions.calculateRect(canvas, xExpr, yExpr, widthExpr, heightExpr)

        val title = "Repairing ${repairProgress.type.label}"
        canvasPopup.draw(dim, title)

        save()

        drawStart(dim, repairProgress)
        tiles.forEach { it.drawTile(dim) }
        drawEnd(dim, repairProgress)

        restore()

        abortButton.draw()
    }

    private fun CanvasRenderingContext2D.drawStart(
        dim: ComponentDimensions,
        repairProgress: RepairProgressMessage
    ) {
        val x = dim.bottomX + 5.vmin
        val y = dim.bottomY - dim.height + 15.vmin + 4.vmin + repairProgress.start * 8.vmin

        save()

        drawUnsolvedLine {
            moveTo(x, y)
            lineTo(x + 5.vmin, y)
        }

        fillStyle = UiStyle.buttonForegroundColor

        beginPath()
        circle(x, y, 2.5.vmin)
        fill()

        restore()

        drawSolvedLine {
            moveTo(x, y)
            lineTo(x + 5.vmin, y)
        }

        fillStyle = "#eee"

        beginPath()
        circle(x, y, 2.vmin)
        fill()

        restore()
    }

    private fun CanvasRenderingContext2D.drawEnd(
        dim: ComponentDimensions,
        repairProgress: RepairProgressMessage
    ) {
        val x = dim.bottomX + dim.width - 5.vmin
        val y = dim.bottomY - dim.height + 15.vmin + 4.vmin + repairProgress.end * 8.vmin

        save()

        drawUnsolvedLine {
            moveTo(x, y)
            lineTo(x - 5.vmin, y)
        }

        fillStyle = UiStyle.buttonForegroundColor

        beginPath()
        circle(x, y, 2.5.vmin)
        fill()

        if (repairProgress.solved) {
            drawSolvedLine {
                moveTo(x, y)
                lineTo(x - 5.vmin, y)
            }

            fillStyle = "#eee"

            beginPath()
            circle(x, y, 2.vmin)
            fill()
        }

        restore()
    }

    private fun parseTiles(repairProgress: RepairProgressMessage) {
        tiles.clear()
        repairProgress.tiles.split(";").forEachIndexed { rowIndex, row ->
            row.split(",").forEachIndexed { columnIndex, tile ->
                tiles += Tile(columnIndex, rowIndex, tile)
            }
        }
    }

    private fun CanvasRenderingContext2D.drawSolvedLine(
        block: CanvasRenderingContext2D.() -> Unit
    ) {
        drawLine("#eee", UiStyle.buttonLineWidth.vmin * 2, block)
    }

    private fun CanvasRenderingContext2D.drawUnsolvedLine(
        block: CanvasRenderingContext2D.() -> Unit
    ) {
        drawLine(UiStyle.buttonForegroundColor, UiStyle.buttonLineWidth.vmin * 4, block)
    }

    private fun CanvasRenderingContext2D.drawLine(
        strokeStyleToUse: String,
        lineWidthToUse: Double,
        block: CanvasRenderingContext2D.() -> Unit
    ) {
        save()

        strokeStyle = strokeStyleToUse
        lineWidth = lineWidthToUse
        lineJoin = CanvasLineJoin.ROUND

        beginPath()
        block()
        stroke()

        restore()
    }

    private val Int.vmin
        get() = canvas.dimensions().vmin * this

    private val Double.vmin
        get() = canvas.dimensions().vmin * this

    private inner class Tile(
        val column: Int,
        val row: Int,
        val connections: String
    ) {

        private var x = 0.0
        private var y = 0.0
        private var width = 0.0
        private var height = 0.0

        fun drawTile(
            dim: ComponentDimensions
        ) {
            width = 8.vmin
            height = 8.vmin
            x = dim.bottomX + 10.vmin + column * width
            y = dim.bottomY - dim.height + 15.vmin + row * height

            with(ctx) {
                drawTileBackground()
                drawTileConnections(connections.contains("+"))
            }
        }

        private fun CanvasRenderingContext2D.drawTileBackground() {
            save()

            strokeStyle = UiStyle.buttonForegroundColor
            lineWidth = UiStyle.buttonLineWidth.vmin * 0.5
            strokeRect(x, y, width, height)

            restore()
        }

        private fun CanvasRenderingContext2D.drawTileConnections(solved: Boolean) {
            save()

            drawUnsolvedLine {
                drawTileConnections()
            }
            if (solved) {
                drawSolvedLine {
                    drawTileConnections()
                }
            }

            restore()
        }

        private fun CanvasRenderingContext2D.drawTileConnections() {
            when {
                connections.contains("0") -> moveTo(x + width * 0.5, y)
                connections.contains("1") -> moveTo(x + width, y + height * 0.5)
                connections.contains("2") -> moveTo(x + width * 0.5, y + height)
                connections.contains("3") -> moveTo(x, y + height * 0.5)
            }
            lineTo(x + width * 0.5, y + height * 0.5)
            when {
                connections.contains("3") -> lineTo(x, y + height * 0.5)
                connections.contains("2") -> lineTo(x + width * 0.5, y + height)
                connections.contains("1") -> lineTo(x + width, y + height * 0.5)
                connections.contains("0") -> lineTo(x + width * 0.5, y)
            }
        }

        fun isInterestedIn(pointerEvent: PointerEvent) =
            pointerEvent.point.x > x && pointerEvent.point.x < x + width &&
                pointerEvent.point.y > y && pointerEvent.point.y < y + height

        fun click() {
            clientSocket.send(Command.CommandSolveRepairGame(column, row))
        }
    }
}
