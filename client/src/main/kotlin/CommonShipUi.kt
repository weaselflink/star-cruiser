import de.bissell.starcruiser.Command
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document

class CommonShipUi {

    private val root = document.getElementById("common-ship-ui")!! as HTMLElement
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val fullScreenButton = root.querySelector(".fullscreen")!! as HTMLButtonElement

    init {
        exitButton.onclick = { clientSocket.send(Command.CommandExitShip) }
        fullScreenButton.onclick = {
            val body = document.querySelector("body")!! as HTMLElement
            if (document.fullscreenElement == null) {
                body.requestFullscreen()
                fullScreenButton.innerText = "Windowed"
            } else {
                document.exitFullscreen()
                fullScreenButton.innerText = "Fullscreen"
            }
        }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }
}