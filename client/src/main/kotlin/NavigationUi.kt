import components.CanvasSlider
import components.MapClick
import components.NavigationMap
import de.bissell.starcruiser.Command.*
import de.bissell.starcruiser.ScanLevel
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import de.bissell.starcruiser.pad
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.math.roundToInt

class NavigationUi : StationUi {

    override val station = Station.Navigation

    private val root = document.getElementById("navigation-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val navigationMap = NavigationMap(canvas) { handleMapClick(it) }
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val addWaypointButton = document.querySelector(".addWaypoint")!! as HTMLButtonElement
    private val deleteWaypointButton = document.querySelector(".deleteWaypoint")!! as HTMLButtonElement
    private val scanShipButton = document.querySelector(".scanShip")!! as HTMLButtonElement
    private val selectionDetails = document.getElementById("selection-details")!! as HTMLElement
    private val detailsScanButton = selectionDetails.querySelector(".detailsScanButton")!! as HTMLButtonElement
    private val detailsDeleteButton = selectionDetails.querySelector(".detailsDeleteButton")!! as HTMLButtonElement
    private val zoomSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 5 },
        yExpr = { it.height - it.vmin * 5 },
        widthExpr = { it.vmin * 40 },
        heightExpr = { it.vmin * 8 },
        onChange = { navigationMap.changeZoom(it) },
        leftText = "Zoom"
    )

    private var buttonState: ButtonState = ButtonState.Initial

    init {
        resize()
        selectionDetails.visibility = Visibility.hidden
        detailsScanButton.onclick = { scanShipClicked() }
        detailsDeleteButton.onclick = { deleteWayPointClicked() }
        mouseEventDispatcher.addHandler(zoomSlider)
        mouseEventDispatcher.addHandler(navigationMap.MapMouseEventHandler())
    }

    override fun show() {
        root.visibility = Visibility.visible
    }

    override fun hide() {
        root.visibility = Visibility.hidden
    }

    fun zoomIn() {
        navigationMap.zoomIn()
    }

    fun zoomOut() {
        navigationMap.zoomOut()
    }

    fun resize() {
        canvas.updateSize()
    }

    fun draw(snapshot: SnapshotMessage.Navigation) {
        drawSelectedDetails()

        with(ctx) {
            resetTransform()
            clear("#000")

            navigationMap.draw(snapshot)
            drawZoom()
        }
    }

    private fun drawSelectedDetails() {
        val selection = selection()
        if (selection != null) {
            this.selectionDetails.visibility = Visibility.visible
            this.selectionDetails.querySelector(".designation")!!.innerHTML =
                selection.label
            this.selectionDetails.querySelector(".bearing")!!.innerHTML =
                selection.bearing.roundToInt().pad(3)
            this.selectionDetails.querySelector(".range")!!.innerHTML =
                selection.range.roundToInt().toString()

            when {
                selection.canScan -> {
                    detailsScanButton.visibility = Visibility.visible
                    detailsDeleteButton.visibility = Visibility.hidden
                }
                selection.canDelete -> {
                    detailsScanButton.visibility = Visibility.hidden
                    detailsDeleteButton.visibility = Visibility.visible
                }
                else -> {
                    detailsScanButton.visibility = Visibility.hidden
                    detailsDeleteButton.visibility = Visibility.hidden
                }
            }
        } else {
            selectionDetails.visibility = Visibility.hidden
            detailsScanButton.visibility = Visibility.hidden
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
        val selectedWaypoint = navigationMap.selectedWaypoint
        if (selectedWaypoint != null) {
            clientSocket.send(CommandDeleteWaypoint(selectedWaypoint.index))
        } else {
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
    }

    fun scanShipClicked() {
        val selectedContact = navigationMap.selectedContact
        if (selectedContact != null && selectedContact.scanLevel != ScanLevel.highest) {
            clientSocket.send(CommandScanShip(selectedContact.id))
        } else {
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
    }

    private fun drawZoom() =
        zoomSlider.draw(1.0 - navigationMap.scaleSetting / 6.0)

    private fun handleMapClick(mapClick: MapClick) {
        when (buttonState) {
            ButtonState.Initial -> mapClick.contacts.firstOrNull()?.also {
                navigationMap.selectedContact = it
            } ?: mapClick.waypoints.firstOrNull()?.also {
                navigationMap.selectedWaypoint = it
            } ?: clearMapSelections()
            ButtonState.AddWaypoint -> {
                clientSocket.send(CommandAddWaypoint(mapClick.world))
                addWaypointButton.removeClass("current")
                buttonState = ButtonState.Initial
            }
            ButtonState.DeleteWaypoint -> {
                mapClick.waypoints.firstOrNull()?.also {
                    clientSocket.send(CommandDeleteWaypoint(it.index))
                    deleteWaypointButton.removeClass("current")
                    buttonState = ButtonState.Initial
                }
            }
            ButtonState.ScanShip -> {
                mapClick.contacts.firstOrNull()?.also {
                    clientSocket.send(CommandScanShip(it.id))
                    scanShipButton.removeClass("current")
                    buttonState = ButtonState.Initial
                }
            }
        }
    }

    private fun clearMapSelections() {
        navigationMap.selectedContact = null
        navigationMap.selectedWaypoint = null
    }

    private fun selection(): SelectionDetails? =
        navigationMap.selectedContact?.let {
            SelectionDetails(
                label = it.designation,
                bearing = it.bearing,
                range = it.relativePosition.length(),
                canScan = it.scanLevel != ScanLevel.highest,
                canDelete = false
            )
        } ?: navigationMap.selectedWaypoint?.let {
            SelectionDetails(
                label = it.name,
                bearing = it.bearing,
                range = it.relativePosition.length(),
                canScan = false,
                canDelete = true
            )
        }

    enum class ButtonState {
        Initial,
        AddWaypoint,
        DeleteWaypoint,
        ScanShip
    }

    data class SelectionDetails(
        val label: String,
        val bearing: Double,
        val range: Double,
        val canScan: Boolean,
        val canDelete: Boolean
    )
}
