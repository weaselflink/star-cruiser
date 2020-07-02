import de.bissell.starcruiser.Command.CommandExitShip
import org.w3c.dom.HTMLButtonElement
import kotlin.browser.document

class DestroyedUi {

    private val root = document.getHtmlElementById("destroyed-ui")
    private val toSelectionButton: HTMLButtonElement = root.byQuery(".toSelection")

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
