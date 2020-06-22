import de.bissell.starcruiser.Command.CommandExitShip
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document

class DestroyedUi {

    private val root = document.getElementById("destroyed-ui")!! as HTMLElement
    private val toSelectionButton = root.querySelector(".toSelection")!! as HTMLButtonElement

    init {
        toSelectionButton.onclick = { toSelection() }
    }

    fun show() {
        if (root.visibility != Visibility.visible) {
            root.visibility = Visibility.visible
        }
    }

    fun hide() {
        if (root.visibility != Visibility.hidden) {
            root.visibility = Visibility.hidden
        }
    }

    private fun toSelection() {
        clientSocket.send(CommandExitShip)
    }
}
