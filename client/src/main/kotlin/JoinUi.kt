import de.bissell.starcruiser.Command
import de.bissell.starcruiser.ShipId
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station.Helm
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.math.max

class JoinUi {

    private val root = document.getElementById("join-ui")!! as HTMLElement

    init {
        document.getElementsByClassName("spawn").asList()
            .map {
                it as HTMLButtonElement
            }.forEach {
                it.onclick = { clientSocket.send(Command.CommandSpawnShip) }
            }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun draw(snapshot: SnapshotMessage.ShipSelection) {
        document.getElementsByClassName("playerShips").asList().forEach { playerShipsList ->
            val listElements = playerShipsList.getElementsByTagName("button")

            val max = max(snapshot.playerShips.size, listElements.length)

            for (index in 0 until max) {
                if (index < snapshot.playerShips.size) {
                    val playerShip = snapshot.playerShips[index]
                    val buttonText = playerShip.name + (playerShip.shipClass?.let { " ($it class)" } ?: "")
                    if (index < listElements.length) {
                        listElements.item(index)!!.let {
                            it as HTMLElement
                        }.apply {
                            if (getAttribute("id") != playerShip.id.id) {
                                setAttribute("id", playerShip.id.id)
                                innerHTML = buttonText
                            }
                        }
                    } else {
                        document.createElement("button").let {
                            it as HTMLElement
                        }.apply {
                            setAttribute("id", playerShip.id.id)
                            addClass("leftEdge")
                            innerHTML = buttonText
                            onclick = { selectPlayerShip(it) }
                        }.also {
                            playerShipsList.appendChild(it)
                        }
                    }
                } else {
                    if (index < listElements.length) {
                        listElements.item(index)?.apply {
                            remove()
                        }
                    }
                }
            }
        }
    }

    private fun selectPlayerShip(event: MouseEvent) {
        clientSocket.apply {
            val target = event.target as HTMLElement
            val shipId = ShipId(target.getAttribute("id")!!)
            send(Command.CommandJoinShip(shipId = shipId, station = Helm))
        }
    }
}
