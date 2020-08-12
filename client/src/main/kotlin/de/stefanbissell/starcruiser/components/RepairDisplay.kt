package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.RepairProgressMessage
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.send
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

class RepairDisplay(
    val canvas: HTMLCanvasElement,
    val xExpr: (CanvasDimensions) -> Double = { it.width * 0.5 - it.vmin * 37 },
    val yExpr: (CanvasDimensions) -> Double = { it.height * 0.5 + it.vmin * 17 },
    val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 74 },
    val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 34 }
) : PointerEventHandler {

    private val ctx = canvas.context2D
    private var visible = false
    private val tiles = mutableListOf<Tile>()

    override fun isInterestedIn(pointerEvent: PointerEvent) = visible

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        tiles.firstOrNull {
            it.isInterestedIn(pointerEvent)
        }?.click()
    }

    fun draw(powerSettings: PowerMessage) {
        if (powerSettings.repairProgress != null) {
            visible = true
            parseTiles(powerSettings.repairProgress!!)
            ctx.draw()
        } else {
            visible = false
        }
    }

    private fun parseTiles(repairProgress: RepairProgressMessage) {
        tiles.clear()
        repairProgress.tiles.split(";").forEachIndexed { rowIndex, row ->
            row.split(",").forEachIndexed { columnIndex, tile ->
                tiles += Tile(columnIndex, rowIndex, tile)
            }
        }
    }

    private fun CanvasRenderingContext2D.draw() {
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

        tiles.forEach { it.drawTile(dim) }

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
            x = dim.bottomX + 5.vmin + column * width
            y = dim.bottomY - dim.height + 5.vmin + row * height

            with(ctx) {
                drawTileBackground()
                drawTileConnections()
            }
        }

        private fun CanvasRenderingContext2D.drawTileBackground() {
            save()

            strokeStyle = UiStyle.buttonForegroundColor
            lineWidth = UiStyle.buttonLineWidth.vmin * 0.5
            strokeRect(x, y, width, height)

            restore()
        }

        private fun CanvasRenderingContext2D.drawTileConnections() {
            save()

            lineWidth = UiStyle.buttonLineWidth.vmin
            if (connections.contains("0")) {
                beginPath()
                moveTo(x + width * 0.5, y + height * 0.5)
                lineTo(x + width * 0.5, y)
                stroke()
            }
            if (connections.contains("1")) {
                beginPath()
                moveTo(x + width * 0.5, y + height * 0.5)
                lineTo(x + width, y + height * 0.5)
                stroke()
            }
            if (connections.contains("2")) {
                beginPath()
                moveTo(x + width * 0.5, y + height * 0.5)
                lineTo(x + width * 0.5, y + height)
                stroke()
            }
            if (connections.contains("3")) {
                beginPath()
                moveTo(x + width * 0.5, y + height * 0.5)
                lineTo(x, y + height * 0.5)
                stroke()
            }

            restore()
        }

        fun isInterestedIn(pointerEvent: PointerEvent) =
            pointerEvent.point.x > x && pointerEvent.point.x < x + width &&
                pointerEvent.point.y > y && pointerEvent.point.y < y + height

        fun click() {
            clientSocket.send(Command.CommandSolveRepairGame(column, row))
        }
    }
}
