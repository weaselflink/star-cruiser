import components.CanvasSlider
import de.bissell.starcruiser.*
import de.bissell.starcruiser.Command.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationUi : StationUi {

    override val station = Station.Navigation

    private val gridSize = 1000.0
    private val root = document.getElementById("navigation-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val addWaypointButton = document.querySelector(".addWaypoint")!! as HTMLButtonElement
    private val deleteWaypointButton = document.querySelector(".deleteWaypoint")!! as HTMLButtonElement
    private val scanShipButton = document.querySelector(".scanShip")!! as HTMLButtonElement
    private val zoomSlider = CanvasSlider(
        canvas = canvas,
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
    private var buttonState: ButtonState = ButtonState.Initial
    private var contacts: List<ContactMessage> = emptyList()
    private var waypoints: List<WaypointMessage> = emptyList()

    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    init {
        resize()
        mouseEventDispatcher.addHandler(zoomSlider)
        mouseEventDispatcher.addHandler(MapMouseEventHandler())
    }

    override fun show() {
        root.style.visibility = "visible"
    }

    override fun hide() {
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
            drawZoom()
            drawScanProgress(ship)
        }
    }

    fun addWayPointClicked() {
        buttonState = if (buttonState != ButtonState.AddWaypoint) {
            ButtonState.AddWaypoint
        } else {
            ButtonState.Initial
        }
        addWaypointButton.removeClass("current")
        deleteWaypointButton.removeClass("current")
        scanShipButton.removeClass("current")
        if (buttonState == ButtonState.AddWaypoint) {
            addWaypointButton.addClass("current")
        }
    }

    fun deleteWayPointClicked() {
        buttonState = if (buttonState != ButtonState.DeleteWaypoint) {
            ButtonState.DeleteWaypoint
        } else {
            ButtonState.Initial
        }
        addWaypointButton.removeClass("current")
        deleteWaypointButton.removeClass("current")
        scanShipButton.removeClass("current")
        if (buttonState == ButtonState.DeleteWaypoint) {
            deleteWaypointButton.addClass("current")
        }
    }

    fun scanShipClicked() {
        buttonState = if (buttonState != ButtonState.ScanShip) {
            ButtonState.ScanShip
        } else {
            ButtonState.Initial
        }
        addWaypointButton.removeClass("current")
        deleteWaypointButton.removeClass("current")
        scanShipButton.removeClass("current")
        if (buttonState == ButtonState.ScanShip) {
            scanShipButton.addClass("current")
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

    private fun drawZoom() =
        zoomSlider.draw(1.0 - scaleSetting / 6.0)

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

    private fun Vector2.adjustForMap() =
        ((this - center) * scale).let { Vector2(it.x, -it.y) }

    inner class MapMouseEventHandler : MouseEventHandler {

        private var lastEvent: Vector2? = null

        override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
            when (buttonState) {
                ButtonState.AddWaypoint -> {
                    clientSocket.send(CommandAddWaypoint(mouseEvent.toWorld()))
                    addWaypointButton.removeClass("current")
                    buttonState = ButtonState.Initial
                }
                ButtonState.DeleteWaypoint -> {
                    getNearestWaypoint(mouseEvent)?.also {
                        clientSocket.send(CommandDeleteWaypoint(it.index))
                        deleteWaypointButton.removeClass("current")
                        buttonState = ButtonState.Initial
                    }
                }
                ButtonState.ScanShip -> {
                    getNearestContact(mouseEvent)?.also {
                        clientSocket.send(CommandScanShip(it.id))
                        scanShipButton.removeClass("current")
                        buttonState = ButtonState.Initial
                    }
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

        private fun getNearestWaypoint(mouseEvent: MouseEvent): WaypointMessage? =
            getNearest(waypoints, mouseEvent)

        private fun getNearestContact(mouseEvent: MouseEvent): ContactMessage? =
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

        private fun Vector2.convert() = (this / scale).let { Vector2(-it.x, it.y) }

        private fun MouseEvent.toWorld() =
            (fromCenterCanvas() / scale).let { Vector2(it.x, -it.y) } + center

        private fun MouseEvent.fromCenterCanvas() =
            toVector2() - Vector2(canvas.width / 2.0, canvas.height / 2.0)

        private fun MouseEvent.toVector2() = Vector2(offsetX, offsetY)
    }

    enum class ButtonState {
        Initial,
        AddWaypoint,
        DeleteWaypoint,
        ScanShip
    }

}
