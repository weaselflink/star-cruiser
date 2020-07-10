package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.pad
import de.stefanbissell.starcruiser.px
import de.stefanbissell.starcruiser.translateToCenter
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.TOP

class MapGrid(
    private val canvas: HTMLCanvasElement,
    private val majorGridSize: Double = 1000.0,
    private val centerIndexX: Int = 50,
    private val centerIndexY: Int = 13
) {

    private val ctx = canvas.context2D

    fun draw(center: Vector2, scale: Double) {
        MapGridRenderer(canvas.dimensions(), center, scale).render()
    }

    private inner class MapGridRenderer(
        private val dim: CanvasDimensions,
        private val center: Vector2,
        private val scale: Double
    ) {

        fun render() {
            ctx.draw()
        }

        private fun CanvasRenderingContext2D.draw() {
            save()
            translateToCenter()
            gridStyle()
            visibleGridSquares().forEach {
                drawSquare(it)
            }
            restore()
        }

        private fun CanvasRenderingContext2D.gridStyle() {
            lineWidth = dim.vmin * 0.3
            strokeStyle = "#1d3549"
            fillStyle = "#664400"
            val textSize = 100.0.adjustForMap().toInt()
            font = "bold ${textSize.px} sans-serif"
            textBaseline = CanvasTextBaseline.TOP
            textAlign = CanvasTextAlign.LEFT
        }

        private fun CanvasRenderingContext2D.drawSquare(gridSquare: GridSquare) {
            gridSquare.topLeftAdjusted.let {
                strokeRect(it.x, it.y, majorGridSize.adjustForMap(), majorGridSize.adjustForMap())
                drawGridLabel(gridSquare)
                drawMinorGrid(it)
            }
        }

        private fun CanvasRenderingContext2D.drawGridLabel(gridSquare: GridSquare) {
            if (majorGridSize * scale > dim.vmin * 10) {
                save()
                val gap = 50.0.adjustForMap().toInt()
                fillStyle = "#332200"
                fillText(gridSquare.label(), gridSquare.topLeftAdjusted.x + gap, gridSquare.topLeftAdjusted.y + gap)
                restore()
            }
        }

        private fun CanvasRenderingContext2D.drawMinorGrid(it: Vector2) {
            if (majorGridSize * scale > dim.vmin * 40) {
                (1..9).map { minorX ->
                    (1..9).map { minorY ->
                        val pos = Vector2(
                            it.x + minorX * scale * majorGridSize * 0.1 - dim.vmin * 0.15,
                            it.y + minorY * scale * majorGridSize * 0.1 - dim.vmin * 0.15
                        )
                        fillRect(pos.x, pos.y, dim.vmin * 0.3, dim.vmin * 0.3)
                    }
                }
            }
        }

        private fun Vector2.adjustForMap() =
            ((this - center) * scale).let { Vector2(it.x, -it.y) }

        private fun Double.adjustForMap() = this * scale

        private fun visibleGridSquares(): List<GridSquare> {
            val canvasCenter = Vector2(dim.width * 0.5, dim.height * 0.5)

            return (0..99).flatMap { xIndex ->
                (0..25).map { yIndex ->
                    val topLeft = Vector2(
                        (xIndex - centerIndexX) * majorGridSize,
                        -(yIndex - centerIndexY) * majorGridSize
                    )
                    val bottomRight = topLeft + Vector2(majorGridSize, -majorGridSize)

                    GridSquare(
                        xIndex,
                        yIndex,
                        topLeft,
                        topLeft.adjustForMap(),
                        bottomRight,
                        bottomRight.adjustForMap()
                    )
                }.filter { square ->
                    val topLeft = square.topLeftAdjusted + canvasCenter
                    val bottomRight = square.bottomRightAdjusted + canvasCenter

                    topLeft.x <= dim.width && bottomRight.x >= 0.0 && topLeft.y <= dim.height && bottomRight.y >= 0.0
                }
            }
        }
    }

    private data class GridSquare(
        val xIndex: Int,
        val yIndex: Int,
        val topLeft: Vector2,
        val topLeftAdjusted: Vector2,
        val bottomRight: Vector2,
        val bottomRightAdjusted: Vector2
    ) {

        fun label() = "${'A' + yIndex}${xIndex.pad(2)}"
    }
}
