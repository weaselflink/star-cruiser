import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min

class NavigationUi {

    private val root = document.getElementById("navigation")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement

    private var dim = 100.0

    init {
        resize()
        window.onresize = { resize() }

        exitButton.onclick = { clientSocket?.send(Command.CommandExitShip.toJson()) }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
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
            clearCanvas()
        }
    }

    private fun CanvasRenderingContext2D.clearCanvas() {
        resetTransform()
        fillStyle = "#000"
        fillRect(0.0, 0.0, dim, dim)
    }
}
