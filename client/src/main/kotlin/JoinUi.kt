import de.bissell.starcruiser.Command
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.PlayerShipMessage
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station.Helm
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.math.max
import kotlin.math.min

class JoinUi {

    private val root = document.getElementById("join-ui")!! as HTMLElement
    private val playerShipsList = root.querySelector(".playerShips")!! as HTMLElement
    private val prevButton = root.querySelector(".playerShipsPrev")!! as HTMLButtonElement
    private val nextButton = root.querySelector(".playerShipsNext")!! as HTMLButtonElement
    private var playerShips = emptyList<PlayerShipMessage>()
    private val pageCount
        get() = playerShips.chunked(6).size
    private var page = 0

    init {
        root.querySelector(".spawn")!!.let {
            it as HTMLButtonElement
        }.also {
            it.onclick = { clientSocket.send(Command.CommandSpawnShip) }
        }
        prevButton.onclick = { prev() }
        nextButton.onclick = { next() }
        drawShipList()
    }

    fun show() {
        root.visibility = Visibility.visible
    }

    fun hide() {
        root.visibility = Visibility.hidden
    }

    fun draw(snapshot: SnapshotMessage.ShipSelection) {
        if (snapshot.playerShips == playerShips) return
        playerShips = snapshot.playerShips

        drawShipList()
    }

    private fun drawShipList() {
        page = max(0, min(page, pageCount - 1))

        updatePagination()

        playerShipsList.querySelectorAll("button").asList().forEach {
            (it as HTMLButtonElement).remove()
        }

        if (pageCount < 1) return

        playerShips.chunked(6)[page]
            .forEach { playerShip ->
                document.createElement("button").let {
                    it as HTMLButtonElement
                }.apply {
                    objectId = playerShip.id
                    addClass("leftEdge")
                    innerHTML = playerShip.name + (playerShip.shipClass?.let { " ($it class)" } ?: "")
                    onclick = { selectPlayerShip(it) }
                }.also {
                    playerShipsList.appendChild(it)
                }
            }
    }

    private fun updatePagination() {
        if (page > 0) {
            prevButton.visibility = Visibility.visible
        } else {
            prevButton.visibility = Visibility.hidden
        }

        if (page < pageCount - 1) {
            nextButton.visibility = Visibility.visible
        } else {
            nextButton.visibility = Visibility.hidden
        }
    }

    private fun prev() {
        page = max(page - 1, 0)
        drawShipList()
    }

    private fun next() {
        page = min(page + 1, pageCount - 1)
        drawShipList()
    }

    private fun selectPlayerShip(event: MouseEvent) {
        clientSocket.apply {
            val target = event.target as HTMLElement
            target.objectId?.also {
                send(Command.CommandJoinShip(objectId = it, station = Helm))
            }
        }
    }

    private var HTMLElement.objectId: ObjectId?
        get() = getAttribute("id")?.let { ObjectId(it) }
        set(value) = if (value != null) {
            setAttribute("id", value.id)
        } else {
            removeAttribute("id")
        }
}
