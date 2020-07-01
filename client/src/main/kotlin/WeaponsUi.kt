import components.CanvasButton
import components.ShortRangeScope
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.Command.CommandLockTarget
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import input.PointerEventDispatcher
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document

class WeaponsUi : StationUi {

    override val station = Station.Weapons

    private val root = document.getElementById("weapons-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.context2D
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)
    private val toggleShieldsButton = document.querySelector(".toggleShields")!! as HTMLButtonElement
    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }
    private val lockTargetButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 45 },
        yExpr = { it.height * 0.5 - it.vmin * 38 },
        widthExpr = { it.vmin * 25 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleLockTarget() },
        activated = { selectingTarget },
        text = "Lock on"
    )

    private var shieldsUp = false
    private var selectingTarget = false

    init {
        resize()
        pointerEventDispatcher.addHandler(shortRangeScope.rotateButton)
        pointerEventDispatcher.addHandler(lockTargetButton)
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

    fun toggleShields() {
        clientSocket.send(Command.CommandSetShieldsUp(!shieldsUp))
    }

    fun draw(snapshot: SnapshotMessage.Weapons) {
        shieldsUp = snapshot.ship.shield.up
        if (shieldsUp) {
            toggleShieldsButton.innerHTML = "Shields down"
        } else {
            toggleShieldsButton.innerHTML = "Shields up"
        }

        ctx.draw(snapshot)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Weapons) {
        transformReset()
        clear("#222")

        shortRangeScope.draw(snapshot)
        lockTargetButton.draw()
    }

    private fun contactSelected(targetId: ObjectId) {
        if (selectingTarget) {
            toggleLockTarget()
            clientSocket.send(CommandLockTarget(targetId))
        }
    }

    private fun toggleLockTarget() {
        selectingTarget = !selectingTarget
    }
}
