package components

import CanvasDimensions
import circle
import clear
import de.bissell.starcruiser.*
import dimensions
import drawShipSymbol
import friendlyContactStyle
import lineTo
import moveTo
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import scanProgressStyle
import shipStyle
import translate
import translateToCenter
import unknownContactStyle
import wayPointStyle
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationMap(
    private val canvas: HTMLCanvasElement
) {

    private val gridSize = 1000.0

    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private var dim = CanvasDimensions(100, 100)

    var center = Vector2()
    var scaleSetting = 3
        private set
    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    private var contacts: List<ContactMessage> = emptyList()
    private var waypoints: List<WaypointMessage> = emptyList()

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
            drawShip(ship)
            drawScanProgress(ship)
        }
    }

    private fun CanvasRenderingContext2D.drawGrid() {
        save()
        translateToCenter()
        strokeStyle = "#4682b4"
        (-20..20).forEach { gridX ->
            beginPath()
            moveTo(Vector2(gridX * gridSize, -20_000.0).adjustForMap())
            lineTo(Vector2(gridX * gridSize, +20_000.0).adjustForMap())
            stroke()
        }
        (-20..20).forEach { gridY ->
            beginPath()
            moveTo(Vector2(-20_000.0, gridY * gridSize).adjustForMap())
            lineTo(Vector2(20_000.0, gridY * gridSize).adjustForMap())
            stroke()
        }
        restore()
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

        strokeStyle = "#fa807290"
        translateToCenter()
        translate(contact.position.adjustForMap())
        beginPath()
        circle(0.0, 0.0, dim.vmin * 2.3, 0.0, PI * scanProgress.progress * 2.0)
        stroke()

        restore()
    }

    fun toWorld(mouseEvent: MouseEvent) =
        (mouseEvent.fromCenterCanvas() / scale).let { Vector2(it.x, -it.y) } + center

    fun convert(vector: Vector2) = (vector / scale).let { Vector2(-it.x, it.y) }

    private fun Vector2.adjustForMap() =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }

    fun getNearestWaypoint(mouseEvent: MouseEvent): WaypointMessage? =
        getNearest(waypoints, mouseEvent)

    fun getNearestContact(mouseEvent: MouseEvent): ContactMessage? =
        getNearest(contacts, mouseEvent)

    private fun <T : Positional> getNearest(elements: Iterable<T>, mouseEvent: MouseEvent): T? {
        val click = mouseEvent.toVector2() - canvasCenter()
        return elements
            .map { it to it.position.adjustForMap() }
            .map { it.first to it.second - click }
            .map { it.first to it.second.length() }
            .filter { it.second <= 20.0 }
            .minBy { it.second }
            ?.first
    }

    private fun canvasCenter() = Vector2(canvas.width * 0.5, canvas.height * 0.5)

    private fun MouseEvent.fromCenterCanvas() =
        toVector2() - Vector2(canvas.width / 2.0, canvas.height / 2.0)

    private fun MouseEvent.toVector2() = Vector2(offsetX, offsetY)
}
