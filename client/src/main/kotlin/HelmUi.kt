import components.CanvasSlider
import components.ShortRangeScope
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HelmUi : StationUi {

    override val station = Station.Helm

    private val root = document.getElementById("helm-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val rotateScopeButton = document.querySelector(".rotateScope")!! as HTMLButtonElement
    private val shortRangeScope = ShortRangeScope(canvas)
    private val throttleSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { max(it.vmin * 5, (it.width - it.min) * 0.5 - it.vmin * 7) },
        yExpr = { it.height - it.vmin * 5 },
        widthExpr = { it.vmin * 8 },
        heightExpr = { it.vmin * 40 },
        onChange = {
            val throttle = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(Command.CommandChangeThrottle(throttle))
        },
        lines = listOf(0.5),
        leftText = "Impulse"
    )
    private val rudderSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { min(it.width - it.vmin * 45, it.width - (it.width - it.min) * 0.5 - it.min * 0.2) },
        yExpr = { it.height - it.vmin * 5 },
        widthExpr = { it.vmin * 40 },
        heightExpr = { it.vmin * 8 },
        onChange = {
            val rudder = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(Command.CommandChangeRudder(rudder))
        },
        lines = listOf(0.5),
        leftText = "Rudder",
        reverseValue = true
    )

    init {
        resize()
        mouseEventDispatcher.addHandler(throttleSlider)
        mouseEventDispatcher.addHandler(rudderSlider)
    }

    fun resize() {
        canvas.updateSize()
    }

    override fun show() {
        root.style.visibility = "visible"
    }

    override fun hide() {
        root.style.visibility = "hidden"
    }

    fun toggleRotateScope() {
        shortRangeScope.toggleRotating()
        rotateScopeButton.removeClass("current")
        if (shortRangeScope.rotating) {
            rotateScopeButton.addClass("current")
        }
    }

    fun draw(snapshot: SnapshotMessage.Helm) {
        val ship = snapshot.ship

        ctx.draw(snapshot, ship)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Helm, ship: ShipMessage) {
        resetTransform()
        clear("#222")

        shortRangeScope.draw(snapshot)

        drawThrottle(ship)
        drawRudder(ship)
    }

    private fun drawThrottle(ship: ShipMessage) =
        throttleSlider.draw((ship.throttle + 100) / 200.0)

    private fun drawRudder(ship: ShipMessage) =
        rudderSlider.draw((ship.rudder + 100) / 200.0)
}
