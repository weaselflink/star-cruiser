import de.bissell.starcruiser.*
import de.bissell.starcruiser.Station.Helm
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationUi {

    private val gridSize = 1000.0

    private val root = document.getElementById("navigation")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val toHelmButton = root.querySelector(".switchToHelm")!! as HTMLButtonElement
    private val zoomSlider = CanvasSlider(
        xExpr = { it.dim * 0.05 },
        yExpr = { it.dim - it.dim * 0.05 },
        widthExpr = { it.dim * 0.05 * 8.0 },
        heightExpr = { it.dim * 0.05 * 1.6 }
    )

    private var dim = 100.0
    private var center = Vector2()
    private var scaleSetting = 3

    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    init {
        resize()
        canvas.onclick = { handleClick(it) }

        exitButton.onclick = { clientSocket.send(Command.CommandExitShip) }
        toHelmButton.onclick = { clientSocket.send(Command.CommandChangeStation(Helm)) }
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

    private fun handleClick(event: MouseEvent) {
        if (zoomSlider.isClickInside(canvas, event)) {
            scaleSetting = (6.0 - zoomSlider.clickValue(canvas, event) * 6.0).roundToInt()
        }
    }

    fun resize() {
        val windowWidth: Int = window.innerWidth
        val windowHeight: Int = window.innerHeight

        with(canvas) {
            if (width != windowWidth || height != windowHeight) {
                width = windowWidth
                height = windowHeight
            }

            style.left = 0.px
            style.top = 0.px
            style.width = windowWidth.px
            style.height = windowHeight.px
        }
    }

    fun draw(ship: ShipMessage, stateCopy: GameStateMessage) {
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
            moveTo(gridX * gridSize * scale, -20_000.0 * scale)
            lineTo(gridX * gridSize * scale, 20_000.0 * scale)
            stroke()
        }
        (-20..20).forEach { gridY ->
            beginPath()
            moveTo(-20_000.0 * scale, gridY * gridSize * scale)
            lineTo(20_000.0 * scale, gridY * gridSize * scale)
            stroke()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        save()
        translateToCenter()
        translate(ship.position.adjustForMap())
        strokeStyle = "#1e90ff"
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
        strokeStyle = "#4682B4"
        fillStyle = "#4682B4"
        lineWidth = 3.0
        val textSize = 16
        font = "bold ${textSize.px} sans-serif"
        textAlign = CanvasTextAlign.CENTER

        for (waypoint in ship.waypoints) {
            save()

            translate(waypoint.position.adjustForMap())
            beginPath()
            circle(0.0, 0.0, 6.0)
            stroke()

            translate(0.0, -20.0)
            fillText("WP${waypoint.index}", 0.0, 0.0)

            restore()
        }
        restore()
    }

    private fun drawZoom() =
        zoomSlider.draw(canvas, 1.0 - scaleSetting / 6.0)

    private fun Vector2.adjustForMap() =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }
}