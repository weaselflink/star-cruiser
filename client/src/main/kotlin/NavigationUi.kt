import components.CanvasSlider
import de.bissell.starcruiser.*
import de.bissell.starcruiser.Command.CommandAddWaypoint
import de.bissell.starcruiser.Command.CommandDeleteWaypoint
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationUi {

    private val gridSize = 1000.0

    private val root = document.getElementById("navigation-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val addWaypointButton = document.querySelector(".addWaypoint")!! as HTMLButtonElement
    private val deleteWaypointButton = document.querySelector(".deleteWaypoint")!! as HTMLButtonElement
    private val zoomSlider = CanvasSlider(
        xExpr = { it.vmin * 5 },
        yExpr = { it.height - it.vmin * 5 },
        widthExpr = { it.vmin * 40 },
        heightExpr = { it.vmin * 8 },
        onChange = { changeZoom(it) },
        leftText = "Zoom"
    )

    private var dim = CanvasDimensions(100, 100)
    private var center = Vector2()
    private var scaleSetting = 3
    private var addingWaypoint = false
    private var deletingWaypoint = false

    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    init {
        resize()
        mouseEventDispatcher.addHandler(zoomSlider)
        mouseEventDispatcher.addHandler(MapMouseEventHandler())
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun zoomIn() {
        scaleSetting = (scaleSetting - 1).clamp(0, 6)
    }

    fun zoomOut() {
        scaleSetting = (scaleSetting + 1).clamp(0, 6)
    }

    private fun changeZoom(value: Double) {
        scaleSetting = (6.0 - value * 6.0).roundToInt()
    }

    fun resize() {
        canvas.updateSize(square = false)
    }

    fun draw(snapshot: SnapshotMessage.Navigation) {
        val ship = snapshot.ship
        dim = canvas.dimensions()

        with(ctx) {
            resetTransform()
            clear("#000")

            drawGrid()
            drawHistory(ship)
            drawWaypoints(ship)
            snapshot.contacts.forEach {
                ctx.drawContact(it)
            }
            drawShip(ship)
            drawZoom()
        }
    }

    fun addWayPointClicked() {
        addingWaypoint = !addingWaypoint
        deletingWaypoint = false
        addWaypointButton.removeClass("current")
        deleteWaypointButton.removeClass("current")
        if (addingWaypoint) {
            addWaypointButton.addClass("current")
        }
    }

    fun deleteWayPointClicked() {
        addingWaypoint = false
        deletingWaypoint = !deletingWaypoint
        addWaypointButton.removeClass("current")
        deleteWaypointButton.removeClass("current")
        if (deletingWaypoint) {
            deleteWaypointButton.addClass("current")
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

    private fun CanvasRenderingContext2D.drawContact(contact: ContactMessage) {
        save()
        translateToCenter()
        contactStyle(dim)

        translate(contact.position.adjustForMap())
        beginPath()
        drawShipSymbol(contact.rotation, dim.vmin * 0.8)

        translate(0.0, -dim.vmin * 2)
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

    private fun drawZoom() =
        zoomSlider.draw(canvas, 1.0 - scaleSetting / 6.0)

    private fun Vector2.adjustForMap() =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }

    inner class MapMouseEventHandler : MouseEventHandler {

        private var lastEvent: Vector2? = null

        override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            when {
                addingWaypoint -> {
                    clientSocket.send(CommandAddWaypoint(mouseEvent.toWorld()))
                    addWaypointButton.removeClass("current")
                    addingWaypoint = false
                }
                deletingWaypoint -> {
                    clientSocket.send(CommandDeleteWaypoint(mouseEvent.toWorld()))
                    deleteWaypointButton.removeClass("current")
                    deletingWaypoint = false
                }
                else -> lastEvent = Vector2(mouseEvent.offsetX, mouseEvent.offsetY)
            }
        }

        override fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            val currentEvent = Vector2(mouseEvent.offsetX, mouseEvent.offsetY)
            lastEvent?.let {
                center += (currentEvent - it).convert()
            }
            lastEvent = currentEvent
        }

        override fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            lastEvent = null
        }

        private fun Vector2.convert() = (this / scale).let { Vector2(-it.x, it.y) }

        private fun MouseEvent.toWorld() =
            (fromCenterCanvas() / scale).let { Vector2(it.x, -it.y) } + center

        private fun MouseEvent.fromCenterCanvas() =
            Vector2(offsetX, offsetY) - Vector2(canvas.width / 2.0, canvas.height / 2.0)
    }
}
