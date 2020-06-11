import components.ShortRangeScope
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.Command.CommandLockTarget
import de.bissell.starcruiser.ShipId
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

class WeaponsUi : StationUi {

    override val station = Station.Weapons

    private val root = document.getElementById("weapons-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val rotateScopeButton = document.querySelector(".rotateScope")!! as HTMLButtonElement
    private val lockTargetButton = document.querySelector(".lockTarget")!! as HTMLButtonElement
    private val toggleShieldsButton = document.querySelector(".toggleShields")!! as HTMLButtonElement
    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }

    private var shieldsUp = false
    private var selectingTarget = false

    init {
        resize()
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

    fun toggleLockTarget() {
        selectingTarget = !selectingTarget
        lockTargetButton.removeClass("current")
        if (selectingTarget) {
            lockTargetButton.addClass("current")
        }
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
        resetTransform()
        clear("#222")

        shortRangeScope.draw(snapshot)
    }

    private fun contactSelected(targetId: ShipId) {
        if (selectingTarget) {
            toggleLockTarget()
            clientSocket.send(CommandLockTarget(targetId))
        }
    }
}
