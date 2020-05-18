import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Station.Helm
import de.bissell.starcruiser.Vector2
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min

class NavigationUi {

    private val gridSize = 1000.0

    private val root = document.getElementById("navigation")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val toHelmButton = root.querySelector(".switchToHelm")!! as HTMLButtonElement

    private var dim = 100.0
    private var center = Vector2()
    private var scale = 1.0 / 4.0

    init {
        resize()
        window.onresize = { resize() }

        exitButton.onclick = { clientSocket?.send(Command.CommandExitShip) }
        toHelmButton.onclick = { clientSocket?.send(Command.CommandChangeStation(Helm)) }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun zoomIn() {
        scale = min(1.0, scale * 2.0)
    }

    fun zoomOut() {
        scale = max(1.0 / 16.0, scale * 0.5)
    }

    private fun resize() {
        val windowWidth: Int = window.innerWidth
        val windowHeight: Int = window.innerHeight

        with (canvas) {
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
            drawShip(ship)
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

    private fun Vector2.adjustForMap() =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }
}
