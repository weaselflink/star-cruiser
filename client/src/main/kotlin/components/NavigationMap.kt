package components

import CanvasDimensions
import MouseEventHandler
import circle
import clear
import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.ContactType
import de.bissell.starcruiser.Positional
import de.bissell.starcruiser.ScanLevel
import de.bissell.starcruiser.ShipId
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.WaypointMessage
import de.bissell.starcruiser.clamp
import dimensions
import drawLockMarker
import drawShipSymbol
import friendlyContactStyle
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import scanProgressStyle
import selectionMarkerStyle
import shipStyle
import translate
import translateToCenter
import unknownContactStyle
import wayPointStyle
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationMap(
    private val canvas: HTMLCanvasElement,
    private val mapClickListener: (MapClick) -> Unit = {}
) {

    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private var dim = CanvasDimensions(100, 100)
    private val mapGrid = MapGrid(canvas)

    var center = Vector2()
    var scaleSetting = 3
        private set
    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    private var selectedShipId: ShipId? = null
    var selectedContact: ContactMessage?
        get() = contacts.firstOrNull { it.id == selectedShipId }
        set(value) { selectedShipId = value?.id?.also { selectedWaypointIndex = null } }

    private var selectedWaypointIndex: Int? = null
    var selectedWaypoint: WaypointMessage?
        get() = waypoints.firstOrNull { it.index == selectedWaypointIndex }
        set(value) { selectedWaypointIndex = value?.index?.also { selectedShipId = null } }

    private var contacts: List<ContactMessage> = emptyList()
    private var waypoints: List<WaypointMessage> = emptyList()

    val selection: Selection?
        get() = selectedContact?.let {
            Selection(
                label = it.designation,
                bearing = it.bearing,
                range = it.relativePosition.length(),
                canScan = it.scanLevel != ScanLevel.highest,
                canDelete = false
            )
        } ?: selectedWaypoint?.let {
            Selection(
                label = it.name,
                bearing = it.bearing,
                range = it.relativePosition.length(),
                canScan = false,
                canDelete = true
            )
        }

    fun zoomIn() {
        scaleSetting = (scaleSetting - 1).clamp(0, 6)
    }

    fun zoomOut() {
        scaleSetting = (scaleSetting + 1).clamp(0, 6)
    }

    fun changeZoom(value: Double) {
        scaleSetting = (6.0 - value * 6.0).roundToInt()
    }

    fun draw(snapshot: SnapshotMessage.Navigation) {
        val ship = snapshot.ship
        contacts = snapshot.contacts
        waypoints = ship.waypoints
        dim = canvas.dimensions()

        with(ctx) {
            resetTransform()
            clear("#000")

            drawGrid()
            drawHistory(ship)
            drawWaypoints(ship)
            drawContacts(snapshot)
            drawSelectedMarker()
            drawShip(ship)
            drawScanProgress(ship)
        }
    }

    private fun drawGrid() {
        mapGrid.draw(center, scale)
    }

    private fun CanvasRenderingContext2D.drawContacts(snapshot: SnapshotMessage.Navigation) {
        snapshot.contacts.forEach {
            drawContact(it)
        }
    }

    private fun CanvasRenderingContext2D.drawContact(contact: ContactMessage) {
        save()
        translateToCenter()
        when (contact.type) {
            ContactType.Friendly -> friendlyContactStyle(dim)
            else -> unknownContactStyle(dim)
        }

        translate(contact.position.adjustForMap())
        drawShipSymbol(contact.rotation, dim.vmin * 0.8)

        translate(0.0, -dim.vmin * 3)
        fillText(contact.designation, 0.0, 0.0)
        restore()
    }

    private fun CanvasRenderingContext2D.drawSelectedMarker() {
        val pos = selectedContact?.position ?: selectedWaypoint?.position

        pos?.also {
            save()
            selectionMarkerStyle(dim)
            translateToCenter()
            translate(pos.adjustForMap())
            drawLockMarker(dim.vmin * 3)
            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        save()
        translateToCenter()
        translate(ship.position.adjustForMap())
        shipStyle(dim)
        drawShipSymbol(ship.rotation, dim.vmin * 0.8)
        restore()
    }

    private fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
        save()
        translateToCenter()
        fillStyle = "#222"

        for (point in ship.history) {
            save()
            translate(point.second.adjustForMap())
            beginPath()
            circle(0.0, 0.0, 2.0)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(ship: ShipMessage) {
        save()
        translateToCenter()
        wayPointStyle(dim)

        for (waypoint in ship.waypoints) {
            save()

            translate(waypoint.position.adjustForMap())
            beginPath()
            circle(0.0, 0.0, dim.vmin * 0.8)
            stroke()

            translate(0.0, -dim.vmin * 2)
            fillText(waypoint.name, 0.0, 0.0)

            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawScanProgress(ship: ShipMessage) {
        val scanProgress = ship.scanProgress ?: return
        val contact = contacts.firstOrNull { it.id == scanProgress.targetId } ?: return

        save()
        scanProgressStyle(dim)

        val designation = contact.designation
        fillText("Scanning $designation", dim.width * 0.5, dim.vmin * 4)

        strokeRect(
            dim.width * 0.5 - dim.vmin * 20, dim.vmin * 10,
            dim.vmin * 40, dim.vmin * 6
        )
        fillRect(
            dim.width * 0.5 - dim.vmin * 19, dim.vmin * 11,
            dim.vmin * 38 * scanProgress.progress, dim.vmin * 4
        )

        strokeStyle = "#ff6347"
        translateToCenter()
        translate(contact.position.adjustForMap())
        beginPath()
        circle(
            0.0, 0.0, dim.vmin * 2.3,
            -PI * 0.5, PI * scanProgress.progress * 2.0 - PI * 0.5
        )
        stroke()

        restore()
    }

    private fun getNearestWaypoints(vector: Vector2): List<WaypointMessage> = getNearest(waypoints, vector)

    private fun getNearestContacts(vector: Vector2): List<ContactMessage> = getNearest(contacts, vector)

    private fun <T : Positional> getNearest(elements: Iterable<T>, vector: Vector2): List<T> {
        val click = vector - canvasCenter()
        return elements
            .asSequence()
            .map { it to it.position.adjustForMap() }
            .map { it.first to it.second - click }
            .map { it.first to it.second.length() }
            .filter { it.second <= 20.0 }
            .sortedBy { it.second }
            .map { it.first }
            .toList()
    }

    private fun Vector2.toWorld() = ((this - canvasCenter()) / scale).let { Vector2(it.x, -it.y) } + center

    private fun convert(vector: Vector2) = (vector / scale).let { Vector2(-it.x, it.y) }

    private fun Vector2.adjustForMap() = ((this - center) * scale).let { Vector2(it.x, -it.y) }

    private fun canvasCenter() = Vector2(canvas.width * 0.5, canvas.height * 0.5)

    private fun MouseEvent.toVector2() = Vector2(offsetX, offsetY)

    inner class MapMouseEventHandler : MouseEventHandler {

        private var firstEvent: Vector2? = null
        private var previousEvent: Vector2? = null
        private var distance = 0.0

        override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            firstEvent = mouseEvent.toVector2()
            previousEvent = mouseEvent.toVector2()
        }

        override fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            handleMove(mouseEvent)
        }

        override fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            handleMove(mouseEvent)
            if (distance <= 20.0) {
                handleClick()
            }
            firstEvent = null
            previousEvent = null
            distance = 0.0
        }

        private fun handleMove(mouseEvent: MouseEvent) {
            val currentEvent = mouseEvent.toVector2()
            previousEvent?.let {
                val move = currentEvent - it
                center += convert(move)
                distance += move.length()
            }
            previousEvent = currentEvent
        }

        private fun handleClick() {
            firstEvent?.let {
                mapClickListener(
                    MapClick(
                        screen = it,
                        world = it.toWorld(),
                        waypoints = getNearestWaypoints(it),
                        contacts = getNearestContacts(it)
                    )
                )
            }
        }
    }
}

data class MapClick(
    val screen: Vector2,
    val world: Vector2,
    val waypoints: List<WaypointMessage>,
    val contacts: List<ContactMessage>
)

data class Selection(
    val label: String,
    val bearing: Double,
    val range: Double,
    val canScan: Boolean,
    val canDelete: Boolean
)
