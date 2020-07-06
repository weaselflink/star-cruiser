import components.CanvasButton
import components.HullDisplay
import components.ShieldsDisplay
import components.ShortRangeScope
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.Command.CommandLockTarget
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import input.PointerEventDispatcher
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.browser.document

class WeaponsUi : StationUi {

    override val station = Station.Weapons

    private val root = document.getHtmlElementById("weapons-ui")
    private val canvas = root.canvas
    private val ctx = canvas.context2D
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)
    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }
    private val lockTargetButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 45 },
        yExpr = { it.height * 0.5 - it.vmin * 38 },
        widthExpr = { it.vmin * 25 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleLockTarget() },
        activated = { selectingTarget },
        text = { "Lock on" }
    )
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 23 }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 15 }
    )
    private val shieldsButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.vmin * 13 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleShields() },
        text = { if (shieldsUp) "Down" else "Up" }
    )

    private var shieldsUp = false
    private var selectingTarget = false

    init {
        resize()
        pointerEventDispatcher.addHandler(shortRangeScope.rotateButton)
        pointerEventDispatcher.addHandler(lockTargetButton)
        pointerEventDispatcher.addHandler(shieldsButton)
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

    fun draw(snapshot: SnapshotMessage.Weapons) {
        shieldsUp = snapshot.ship.shield.up

        ctx.draw(snapshot)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Weapons) {
        transformReset()
        clear("#222")

        shortRangeScope.draw(snapshot)
        lockTargetButton.draw()
        hullDisplay.draw(snapshot.ship)
        shieldsDisplay.draw(snapshot.ship.shield)
        shieldsButton.draw()
    }

    private fun contactSelected(targetId: ObjectId) {
        if (selectingTarget) {
            toggleLockTarget()
            clientSocket.send(CommandLockTarget(targetId))
        }
    }

    private fun toggleShields() {
        clientSocket.send(Command.CommandSetShieldsUp(!shieldsUp))
    }

    private fun toggleLockTarget() {
        selectingTarget = !selectingTarget
    }
}
