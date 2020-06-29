import components.CanvasButton
import components.CanvasSlider
import components.JumpDisplay
import components.ShortRangeScope
import de.bissell.starcruiser.Command.CommandChangeJumpDistance
import de.bissell.starcruiser.Command.CommandChangeRudder
import de.bissell.starcruiser.Command.CommandChangeThrottle
import de.bissell.starcruiser.Command.CommandStartJump
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import input.PointerEventDispatcher
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
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)
    private val rotateScopeButton = document.querySelector(".rotateScope")!! as HTMLButtonElement
    private val shortRangeScope = ShortRangeScope(canvas)
    private val throttleSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 10 },
        heightExpr = { it.vmin * 60 },
        onChange = {
            val throttle = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(CommandChangeThrottle(throttle))
        },
        lines = listOf(0.5),
        leftText = "Impulse"
    )
    private val jumpSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 15 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 10 },
        heightExpr = { it.vmin * 60 },
        onChange = {
            clientSocket.send(CommandChangeJumpDistance(it))
        },
        leftText = "Distance"
    )
    private val rudderSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.width - it.vmin * 63 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 60 },
        heightExpr = { it.vmin * 10 },
        onChange = {
            val rudder = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(CommandChangeRudder(rudder))
        },
        lines = listOf(0.5),
        leftText = "Rudder",
        reverseValue = true
    )
    private val jumpDisplay = JumpDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 27 },
        yExpr = { it.height - if (it.width >= it.vmin * 125) it.vmin * 15 else it.vmin * 27 }
    )
    private val jumpButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.vmin * 34 },
        yExpr = { it.height - if (it.width >= it.vmin * 125) it.vmin * 3 else it.vmin * 15 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { clientSocket.send(CommandStartJump) },
        text = "Jump"
    )

    init {
        resize()
        pointerEventDispatcher.addHandler(throttleSlider)
        pointerEventDispatcher.addHandler(jumpSlider)
        pointerEventDispatcher.addHandler(rudderSlider)
        pointerEventDispatcher.addHandler(jumpButton)
    }

    fun resize() {
        canvas.updateSize()
    }

    override fun show() {
        root.visibility = Visibility.visible
    }

    override fun hide() {
        root.visibility = Visibility.hidden
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
        transformReset()
        clear("#222")

        shortRangeScope.draw(snapshot)

        drawThrottle(ship)
        drawJump(ship)
        drawRudder(ship)
    }

    private fun drawThrottle(ship: ShipMessage) =
        throttleSlider.draw((ship.throttle + 100) / 200.0)

    private fun drawJump(ship: ShipMessage) {
        jumpSlider.draw(ship.jumpDrive.ratio)
        jumpDisplay.draw(ship.jumpDrive)
        jumpButton.draw()
    }

    private fun drawRudder(ship: ShipMessage) =
        rudderSlider.draw((ship.rudder + 100) / 200.0)
}
