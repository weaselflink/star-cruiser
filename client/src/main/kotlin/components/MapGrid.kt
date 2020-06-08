package components

import CanvasDimensions
import de.bissell.starcruiser.Vector2
import dimensions
import lineTo
import moveTo
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import translateToCenter

class MapGrid(
    private val canvas: HTMLCanvasElement,
    private val majorGap: Double = 1000.0
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
        strokeStyle = "#4682b4"
        (-20..20).forEach { gridX ->
            beginPath()
            moveTo(Vector2(gridX * majorGap, -20_000.0).adjustForMap(center, scale))
            lineTo(Vector2(gridX * majorGap, +20_000.0).adjustForMap(center, scale))
            stroke()
        }
        (-20..20).forEach { gridY ->
            beginPath()
            moveTo(Vector2(-20_000.0, gridY * majorGap).adjustForMap(center, scale))
            lineTo(Vector2(20_000.0, gridY * majorGap).adjustForMap(center, scale))
            stroke()
        }
        restore()
    }

    private fun Vector2.adjustForMap(center: Vector2, scale: Double) =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }

    private fun Double.adjustForMap(scale: Double) = this * scale
}