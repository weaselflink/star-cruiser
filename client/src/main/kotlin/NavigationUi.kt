import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.clip
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationUi {

    private val gridSize = 1000.0

    private val root = document.getElementById("navigation-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val zoomSlider = CanvasSlider(
        xExpr = { it.dim * 0.05 },
        yExpr = { it.height - it.dim * 0.05 },
        widthExpr = { it.dim * 0.05 * 8.0 },
        heightExpr = { it.dim * 0.05 * 1.6 },
        onChange = { changeZoom(it) }
    )

    private var dim = 100.0
    private var center = Vector2()
    private var scaleSetting = 3

    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    init {
        resize()
        mouseEventDispatcher.addHandler(zoomSlider)
        mouseEventDispatcher.addHandler(MapMouseEventHandler())
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun zoomIn() {
        scaleSetting = (scaleSetting - 1).clip(0, 6)
    }

    fun zoomOut() {
        scaleSetting = (scaleSetting + 1).clip(0, 6)
    }

    private fun changeZoom(value: Double) {
        scaleSetting = (6.0 - value * 6.0).roundToInt()
    }

    fun resize() {
        canvas.updateSize(square = false)
    }

    fun draw(ship: ShipMessage) {
        dim = min(canvas.width, canvas.height).toDouble()

        with(ctx) {
            resetTransform()
            clear("#000")

            drawGrid()
            drawHistory(ship)
            drawWaypoints(ship)
            drawShip(ship)
            drawZoom()
        }
    }

    private fun CanvasRenderingContext2D.drawGrid() {
        save()
        translateToCenter()
        strokeStyle = "#4682b4"
        (-20..20).forEach { gridX ->
            beginPath()
            moveTo(Vector2(gridX * gridSize, -20_000.0).adjustForMap())
            lineTo(Vector2(gridX * gridSize, +20_000.0).adjustForMap())
            stroke()
        }
        (-20..20).forEach { gridY ->
            beginPath()
            moveTo(Vector2(-20_000.0, gridY * gridSize).adjustForMap())
            lineTo(Vector2(20_000.0, gridY * gridSize).adjustForMap())
            stroke()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        save()
        translateToCenter()
        translate(ship.position.adjustForMap())
        shipStyle(dim)
        drawShipSymbol(ship.rotation, dim * 0.008)
        restore()
    }

    private fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
        save()
        translateToCenter()
        fillStyle = "#222"

        for (point in ship.history) {
            save()
            translate(point.second.adjustForMap())
            beginPath()
            circle(0.0, 0.0, 2.0)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(ship: ShipMessage) {
        save()
        translateToCenter()
        wayPointStyle(dim)

        for (waypoint in ship.waypoints) {
            save()

            translate(waypoint.position.adjustForMap())
            beginPath()
            circle(0.0, 0.0, dim * 0.008)
            stroke()

            translate(0.0, -dim * 0.02)
            fillText(waypoint.name, 0.0, 0.0)

            restore()
        }
        restore()
    }

    private fun drawZoom() =
        zoomSlider.draw(canvas, 1.0 - scaleSetting / 6.0)

    private fun Vector2.adjustForMap() =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }

    inner class MapMouseEventHandler : MouseEventHandler {

        private var lastEvent: Vector2? = null

        override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            lastEvent = Vector2(mouseEvent.offsetX, mouseEvent.offsetY)
        }

        override fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            val currentEvent = Vector2(mouseEvent.offsetX, mouseEvent.offsetY)
            lastEvent?.let {
                center += (currentEvent - it).convert()
            }
            lastEvent = currentEvent
        }

        override fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            lastEvent = null
        }

        private fun Vector2.convert() =
            (this / scale).let { Vector2(-it.x, it.y) }
    }
}
