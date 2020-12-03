package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.MapAreaMessage
import de.stefanbissell.starcruiser.MapAsteroidMessage
import de.stefanbissell.starcruiser.MapContactMessage
import de.stefanbissell.starcruiser.MapSelectionMessage
import de.stefanbissell.starcruiser.NavigationShipMessage
import de.stefanbissell.starcruiser.Positional
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.circle
import de.stefanbissell.starcruiser.clear
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawAsteroidSymbol
import de.stefanbissell.starcruiser.drawLockMarker
import de.stefanbissell.starcruiser.drawProjectileSymbol
import de.stefanbissell.starcruiser.drawVesselSymbol
import de.stefanbissell.starcruiser.enemyContactStyle
import de.stefanbissell.starcruiser.environmentContactStyle
import de.stefanbissell.starcruiser.friendlyContactStyle
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.lineTo
import de.stefanbissell.starcruiser.moveTo
import de.stefanbissell.starcruiser.neutralContactStyle
import de.stefanbissell.starcruiser.scanProgressStyle
import de.stefanbissell.starcruiser.selectionMarkerStyle
import de.stefanbissell.starcruiser.sensorRangeStyle
import de.stefanbissell.starcruiser.shipStyle
import de.stefanbissell.starcruiser.transformReset
import de.stefanbissell.starcruiser.translate
import de.stefanbissell.starcruiser.translateToCenter
import de.stefanbissell.starcruiser.unknownContactStyle
import de.stefanbissell.starcruiser.wayPointStyle
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationMap(
    private val canvas: HTMLCanvasElement,
    private val mapClickListener: (MapClick) -> Unit = {}
) : PointerEventHandlerParent() {

    private val ctx = canvas.context2D
    private var dim = CanvasDimensions(100, 100)
    private val mapGrid = MapGrid(canvas)
    var center = Vector2()
    var scaleSetting = 3
        private set
    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    private var ownPosition = Vector2()
    private var contacts: List<MapContactMessage> = emptyList()
    private var asteroids: List<MapAsteroidMessage> = emptyList()
    private var waypoints: List<WaypointMessage> = emptyList()
    private var mapAreas: List<MapAreaMessage> = emptyList()

    init {
        addChildren(MapPointerEventHandler())
    }

    fun changeZoom(value: Double) {
        scaleSetting = (6.0 - value * 6.0).roundToInt()
    }

    fun centerOnShip() {
        center = ownPosition
    }

    fun draw(snapshot: SnapshotMessage.Navigation) {
        val ship = snapshot.ship
        ownPosition = ship.position
        contacts = snapshot.contacts
        asteroids = snapshot.asteroids
        waypoints = ship.waypoints
        mapAreas = snapshot.mapAreas
        dim = canvas.dimensions()

        with(ctx) {
            transformReset()
            clear(UiStyle.mapBackgroundColor)

            drawGrid()
            drawSensorRange(ship)
            drawHistory(ship)
            drawAsteroids()
            drawWaypoints(ship)
            drawContacts()
            drawSelectedMarker(snapshot.mapSelection)
            drawShip(ship)
            drawScanProgress(ship)
        }
    }

    private fun drawGrid() {
        mapGrid.draw(center, scale)
    }

    private fun CanvasRenderingContext2D.drawSensorRange(ship: NavigationShipMessage) {
        save()
        translateToCenter()
        translate(ship.position.adjustForMap())

        sensorRangeStyle(dim)
        beginPath()
        circle(0.0, 0.0, ship.sensorRange.adjustForMap())
        stroke()

        restore()
    }

    private fun CanvasRenderingContext2D.drawAsteroids() {
        save()
        translateToCenter()
        environmentContactStyle(dim)

        if (scaleSetting < 4) {
            asteroids.forEach {
                drawAsteroid(it)
            }
        } else {
            mapAreas.forEach {
                when (it) {
                    is MapAreaMessage.Polygon -> drawMapAreaPolygon(it)
                    is MapAreaMessage.Circle -> drawMapAreaCircle(it)
                }
            }
        }

        restore()
    }

    private fun CanvasRenderingContext2D.drawAsteroid(asteroid: MapAsteroidMessage) {
        save()
        translate(asteroid.position.adjustForMap())
        drawAsteroidSymbol(asteroid.rotation, (dim.vmin * 0.8 * asteroid.radius * 0.1).adjustForMap())
        restore()
    }

    private fun CanvasRenderingContext2D.drawMapAreaPolygon(mapArea: MapAreaMessage.Polygon) {
        save()
        beginPath()
        mapArea.points.forEachIndexed { index, point ->
            if (index == 0) {
                moveTo(point.adjustForMap())
            } else {
                lineTo(point.adjustForMap())
            }
        }
        closePath()
        fill()
        beginPath()
        mapArea.points.forEachIndexed { index, point ->
            if (index == 0) {
                moveTo(point.adjustForMap())
            } else {
                lineTo(point.adjustForMap())
            }
        }
        closePath()
        stroke()
        restore()
    }

    private fun CanvasRenderingContext2D.drawMapAreaCircle(mapArea: MapAreaMessage.Circle) {
        val center = mapArea.center.adjustForMap()
        val radius = mapArea.radius.adjustForMap()
        save()
        beginPath()
        circle(center.x, center.y, radius)
        fill()
        beginPath()
        circle(center.x, center.y, radius)
        closePath()
        stroke()
        restore()
    }

    private fun CanvasRenderingContext2D.drawContacts() {
        contacts.forEach {
            drawContact(it)
        }
    }

    private fun CanvasRenderingContext2D.drawContact(contact: MapContactMessage) {
        save()
        translateToCenter()
        when (contact.type) {
            ContactType.Friendly -> friendlyContactStyle(dim)
            ContactType.Enemy -> enemyContactStyle(dim)
            ContactType.Neutral -> neutralContactStyle(dim)
            else -> unknownContactStyle(dim)
        }

        translate(contact.position.adjustForMap())
        if (contact.shipType == ShipType.Vessel) {
            drawVesselSymbol(contact.rotation, dim.vmin * 0.8)
        } else {
            drawProjectileSymbol(contact.rotation, dim.vmin * 0.8)
        }

        translate(0.0, -dim.vmin * 3)
        fillText(contact.designation, 0.0, 0.0)
        restore()
    }

    private fun CanvasRenderingContext2D.drawSelectedMarker(mapSelection: MapSelectionMessage?) {
        mapSelection?.position?.also {
            save()
            selectionMarkerStyle(dim)
            translateToCenter()
            translate(it.adjustForMap())
            drawLockMarker(dim.vmin * 3)
            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawShip(ship: NavigationShipMessage) {
        save()
        translateToCenter()
        translate(ship.position.adjustForMap())
        shipStyle(dim)
        drawVesselSymbol(ship.rotation, dim.vmin * 0.8)
        restore()
    }

    private fun CanvasRenderingContext2D.drawHistory(ship: NavigationShipMessage) {
        save()
        translateToCenter()
        fillStyle = "#222"

        for (point in ship.history) {
            save()
            translate(point.adjustForMap())
            beginPath()
            circle(0.0, 0.0, 2.0)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(ship: NavigationShipMessage) {
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

    private fun CanvasRenderingContext2D.drawScanProgress(ship: NavigationShipMessage) {
        val progress = ship.scanProgress ?: return
        val contact = contacts.firstOrNull { it.id == progress.targetId } ?: return

        drawScanProgressMarker(contact)
    }

    private fun CanvasRenderingContext2D.drawScanProgressMarker(contact: MapContactMessage) {
        save()
        scanProgressStyle(dim)

        strokeStyle = "#ff6347"
        translateToCenter()
        translate(contact.position.adjustForMap())
        beginPath()
        circle(0.0, 0.0, dim.vmin * 2.3)
        stroke()

        restore()
    }

    private fun getNearestWaypoints(vector: Vector2): List<WaypointMessage> = getNearest(waypoints, vector)

    private fun getNearestContacts(vector: Vector2): List<MapContactMessage> = getNearest(contacts, vector)

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

    private fun Double.adjustForMap() = this * scale

    private fun Vector2.adjustForMap() = ((this - center) * scale).let { Vector2(it.x, -it.y) }

    private fun canvasCenter() = Vector2(canvas.width * 0.5, canvas.height * 0.5)

    private inner class MapPointerEventHandler : PointerEventHandler {

        private var firstEvent: Vector2? = null
        private var previousEvent: Vector2? = null
        private var distance = 0.0

        override fun handlePointerDown(pointerEvent: PointerEvent) {
            firstEvent = pointerEvent.point
            previousEvent = pointerEvent.point
        }

        override fun handlePointerMove(pointerEvent: PointerEvent) {
            handleMove(pointerEvent.point)
        }

        override fun handlePointerUp(pointerEvent: PointerEvent) {
            handleMove(pointerEvent.point)
            if (distance <= 20.0) {
                handleClick()
            }
            firstEvent = null
            previousEvent = null
            distance = 0.0
        }

        private fun handleMove(currentEvent: Vector2) {
            previousEvent?.let {
                val move = currentEvent - it
                center += convert(move)
                distance += move.length()
            }
            previousEvent = currentEvent
        }

        private fun handleClick() {
            firstEvent
                ?.createClick()
                ?.also {
                    mapClickListener(it)
                }
        }

        private fun Vector2.createClick(): MapClick {
            return MapClick(
                screen = this,
                world = toWorld(),
                waypoints = getNearestWaypoints(this),
                contacts = getNearestContacts(this)
            )
        }
    }
}

data class MapClick(
    val screen: Vector2,
    val world: Vector2,
    val waypoints: List<WaypointMessage>,
    val contacts: List<MapContactMessage>
)
