package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Station.Helm
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.MouseEvent
import kotlin.math.max
import kotlin.math.min

class JoinUi {

    private val root = document.getHtmlElementById("join-ui")
    private val playerShipsList: HTMLElement = root.byQuery(".playerShips")
    private val prevButton: HTMLButtonElement = root.byQuery(".playerShipsPrev")
    private val nextButton: HTMLButtonElement = root.byQuery(".playerShipsNext")
    private var playerShips = emptyList<PlayerShipMessage>()
    private val pageCount
        get() = playerShips.chunked(6).size
    private var page = 0

    init {
        root.byQuery<HTMLButtonElement>(".spawn").also {
            it.onclick = { ClientSocket.send(Command.CommandSpawnShip) }
        }
        prevButton.onclick = { prev() }
        nextButton.onclick = { next() }
        drawShipList()
    }

    fun show() {
        if (root.visibility != Visibility.visible) {
            root.visibility = Visibility.visible

            drawShipList()
        }
    }

    fun hide() {
        if (root.visibility != Visibility.hidden) {
            root.visibility = Visibility.hidden
            prevButton.visibility = Visibility.hidden
            nextButton.visibility = Visibility.hidden
        }
    }

    fun draw(snapshot: SnapshotMessage.ShipSelection) {
        if (snapshot.playerShips != playerShips) {
            playerShips = snapshot.playerShips

            drawShipList()
        }
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
        ClientSocket.apply {
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
