import de.bissell.starcruiser.ClientState
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Station
import de.bissell.starcruiser.Station.*
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

class CommonShipUi {

    private val root = document.getElementById("common-ship-ui")!! as HTMLElement
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val fullScreenButton = root.querySelector(".fullscreen")!! as HTMLButtonElement
    private val stationButtons = mapOf(
        Helm to root.querySelector(".switchToHelm")!! as HTMLButtonElement,
        Navigation to root.querySelector(".switchToNavigation")!! as HTMLButtonElement,
        MainScreen to root.querySelector(".switchToMainScreen")!! as HTMLButtonElement
    )

    private var currentStation: Station = Helm

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

        stationButtons.forEach {
            it.value.onclick = { _ ->
                clientSocket.send(Command.CommandChangeStation(it.key))
            }
        }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun draw(ship: ShipMessage, stateCopy: GameStateMessage) {
        val newStation = when (stateCopy.snapshot.clientState) {
            ClientState.Navigation -> Navigation
            ClientState.MainScreen -> MainScreen
            else -> Helm
        }

        if (newStation != currentStation) {
            stationButtons[currentStation]?.removeClass("current")
            stationButtons[newStation]?.addClass("current")
            currentStation = newStation
        }
    }
}