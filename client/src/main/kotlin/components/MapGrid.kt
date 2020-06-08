package components

import CanvasDimensions
import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.pad
import dimensions
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.TOP
import px
import translateToCenter

class MapGrid(
    private val canvas: HTMLCanvasElement,
    private val majorGridSize: Double = 1000.0
) {

    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private var dim = CanvasDimensions(100, 100)

    fun draw(center: Vector2, scale: Double) {
        dim = canvas.dimensions()

        ctx.draw(center, scale)
    }

    private fun CanvasRenderingContext2D.draw(center: Vector2, scale: Double) {
        save()
        translateToCenter()
        lineWidth = dim.vmin * 0.3
        strokeStyle = "#1d3549"
        fillStyle = "#664400"
        val textSize = 100.0.adjustForMap(scale).toInt()
        font = "bold ${textSize.px} sans-serif"
        textBaseline = CanvasTextBaseline.TOP
        textAlign = CanvasTextAlign.LEFT

        visibleGridSquares(center, scale).forEach {
            drawSquare(it, center, scale)
        }

        restore()
    }

    private fun CanvasRenderingContext2D.drawSquare(gridSquare: GridSquare, center: Vector2, scale: Double) {
        gridSquare.topLeft.adjustForMap(center, scale).let {
            strokeRect(it.x, it.y, majorGridSize.adjustForMap(scale), majorGridSize.adjustForMap(scale))

            save()
            if (majorGridSize * scale > dim.vmin * 10) {
                val gap = 50.0.adjustForMap(scale).toInt()
                fillStyle = "#332200"
                fillText(gridSquare.label(), it.x + gap, it.y + gap)
            }
            restore()

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
    }

    private fun Vector2.adjustForMap(center: Vector2, scale: Double) =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }

    private fun Double.adjustForMap(scale: Double) = this * scale

    private fun visibleGridSquares(center: Vector2, scale: Double): List<GridSquare> {
        val canvasCenter = Vector2(dim.width * 0.5, dim.height * 0.5)
        val centerX = 50
        val centerY = 13

        return (0..99).flatMap { xIndex ->
            (0..25).map { yIndex ->
                val topLeft = Vector2((xIndex - centerX) * majorGridSize, -(yIndex - centerY) * majorGridSize)
                val bottomRight = topLeft + Vector2(majorGridSize, -majorGridSize)

                GridSquare(
                    xIndex,
                    yIndex,
                    topLeft,
                    bottomRight
                )
            }.filter { square ->
                val topLeft = square.topLeft.adjustForMap(center, scale) + canvasCenter
                val bottomRight = square.bottomRight.adjustForMap(center, scale) + canvasCenter

                topLeft.x <= dim.width && bottomRight.x >= 0.0 && topLeft.y <= dim.height && bottomRight.y >= 0.0
            }
        }
    }
}

private data class GridSquare(
    val xIndex: Int,
    val yIndex: Int,
    val topLeft: Vector2,
    val bottomRight: Vector2
) {

    fun label() = "${'A' + yIndex}${xIndex.pad(2)}"
}
