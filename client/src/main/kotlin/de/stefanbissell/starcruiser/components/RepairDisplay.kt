package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ClientSocket
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.RepairProgressMessage
import de.stefanbissell.starcruiser.circle
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND

class RepairDisplay(
    val canvas: HTMLCanvasElement,
    val xExpr: CanvasDimensions.() -> Double = { width * 0.5 - vmin * 48 },
    val yExpr: CanvasDimensions.() -> Double = { height * 0.5 + vmin * 36 },
    val widthExpr: CanvasDimensions.() -> Double = { vmin * 96 },
    val heightExpr: CanvasDimensions.() -> Double = { vmin * 76 }
) : PointerEventHandler {

    private val ctx = canvas.context2D
    private val tiles = mutableListOf<Tile>()

    private val canvasPopup = CanvasPopup(canvas)
    private val abortButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr() + widthExpr() - vmin * 25 },
        yExpr = { yExpr() - vmin * 5 },
        widthExpr = { vmin * 20 },
        onClick = { ClientSocket.send(Command.CommandAbortRepair) },
        initialText = "Abort"
    )

    private var visible = false
    private var currentRepairProgress = someDefaultRepairProgress()

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
            parseTiles()
            ctx.draw(it)
        }
    }

    private fun CanvasRenderingContext2D.draw(repairProgress: RepairProgressMessage) {
        currentRepairProgress = repairProgress
        val dim = ComponentDimensions.calculateRect(
            canvas = canvas,
            xExpr = xExpr,
            yExpr = yExpr,
            widthExpr = widthExpr,
            heightExpr = heightExpr
        )

        val title = "Repairing ${repairProgress.type.label}"
        canvasPopup.draw(dim, title)

        save()

        drawStart(dim)
        tiles.forEach { it.drawTile(dim) }
        drawEnd(dim)

        restore()

        abortButton.draw()
    }

    private fun CanvasRenderingContext2D.drawStart(dim: ComponentDimensions) {
        val x = dim.leftX + (1.vmin + tileSize(dim) * 0.5)
        val y = dim.topY + 15.vmin + tileSize(dim) * 0.5 + currentRepairProgress.start * tileSize(dim)

        save()

        drawUnsolvedLine {
            moveTo(x, y)
            lineTo(x + tileSize(dim) * 0.5, y)
        }

        fillStyle = UiStyle.buttonForegroundColor

        beginPath()
        circle(x, y, tileSize(dim) * 0.3)
        fill()

        restore()

        drawSolvedLine {
            moveTo(x, y)
            lineTo(x + tileSize(dim) * 0.5, y)
        }

        fillStyle = "#eee"

        beginPath()
        circle(x, y, tileSize(dim) * 0.25)
        fill()

        restore()
    }

    private fun CanvasRenderingContext2D.drawEnd(dim: ComponentDimensions) {
        val x = dim.rightX - (1.vmin + tileSize(dim) * 0.5)
        val y = dim.topY + 15.vmin + tileSize(dim) * 0.5 + currentRepairProgress.end * tileSize(dim)

        save()

        drawUnsolvedLine {
            moveTo(x, y)
            lineTo(x - tileSize(dim) * 0.5, y)
        }

        fillStyle = UiStyle.buttonForegroundColor

        beginPath()
        circle(x, y, tileSize(dim) * 0.3)
        fill()

        if (currentRepairProgress.solved) {
            drawSolvedLine {
                moveTo(x, y)
                lineTo(x - tileSize(dim) * 0.5, y)
            }

            fillStyle = "#eee"

            beginPath()
            circle(x, y, tileSize(dim) * 0.25)
            fill()
        }

        restore()
    }

    private fun parseTiles() {
        tiles.clear()
        currentRepairProgress.tiles.split(";").forEachIndexed { rowIndex, row ->
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

    private fun tileSize(
        dim: ComponentDimensions
    ) = (dim.width - 2.vmin) / (currentRepairProgress.width + 2)

    private fun someDefaultRepairProgress() =
        RepairProgressMessage(
            type = PoweredSystemType.Jump,
            width = 7,
            height = 4,
            start = 0,
            end = 0,
            tiles = "",
            solved = false
        )

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
            width = tileSize(dim)
            height = tileSize(dim)
            x = dim.leftX + 1.vmin + (column + 1) * width
            y = dim.topY + 15.vmin + row * height

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
            ClientSocket.send(Command.CommandSolveRepairGame(column, row))
        }
    }
}
