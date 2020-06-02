import components.ShortRangeScope
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.SnapshotMessage
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

class WeaponsUi {

    private val root = document.getElementById("weapons-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val rotateScopeButton = document.querySelector(".rotateScope")!! as HTMLButtonElement
    private val lockTargetButton = document.querySelector(".lockTarget")!! as HTMLButtonElement
    private val shortRangeScope = ShortRangeScope(canvas)

    private var selectingTarget = false

    init {
        resize()
    }

    fun resize() {
        canvas.updateSize(square = true)
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun toggleRotateScope() {
        shortRangeScope.toggleRotating()
        rotateScopeButton.removeClass("current")
        if (shortRangeScope.rotating) {
            rotateScopeButton.addClass("current")
        }
    }

    fun lockTarget() {
        selectingTarget = !selectingTarget
        lockTargetButton.removeClass("current")
        if (selectingTarget) {
            lockTargetButton.addClass("current")
        }
    }

    fun draw(snapshot: SnapshotMessage.Weapons) {
        val ship = snapshot.ship

        ctx.draw(snapshot, ship)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Weapons, ship: ShipMessage) {
        resetTransform()
        clear("#222")

        shortRangeScope.draw(snapshot)
    }
}
