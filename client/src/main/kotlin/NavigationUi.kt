import components.*
import de.bissell.starcruiser.Command.*
import de.bissell.starcruiser.ScanLevel
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import input.PointerEventDispatcher
import kotlin.browser.document

class NavigationUi : StationUi {

    override val station = Station.Navigation

    private val root = document.getHtmlElementById("navigation-ui")
    private val canvas = root.canvas
    private val navigationMap = NavigationMap(canvas) { handleMapClick(it) }
    private val ctx = canvas.context2D
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)
    private val selectionDetails = SelectionDetails(
        onScan = { scanShipClicked() },
        onDelete = { deleteWaypointClicked() }
    )
    private val zoomSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 50 },
        heightExpr = { it.vmin * 10 },
        onChange = { navigationMap.changeZoom(it) },
        leftText = "Zoom"
    )
    private val addWaypointButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.vmin * 55 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 37 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleAddWaypoint() },
        activated = { buttonState == ButtonState.AddWaypoint },
        text = { "Add waypoint" }
    )

    private var buttonState: ButtonState = ButtonState.Initial

    init {
        resize()
        pointerEventDispatcher.addHandlers(
            zoomSlider,
            addWaypointButton,
            navigationMap.MapPointerEventHandler()
        )
    }

    override fun show() {
        root.visibility = Visibility.visible
    }

    override fun hide() {
        root.visibility = Visibility.hidden
        selectionDetails.hide()
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
        selectionDetails.draw(navigationMap.selection)

        with(ctx) {
            transformReset()
            clear("#000")

            navigationMap.draw(snapshot)
            drawZoom()
            addWaypointButton.draw()
        }
    }

    private fun toggleAddWaypoint() {
        buttonState = if (buttonState != ButtonState.AddWaypoint) {
            ButtonState.AddWaypoint
        } else {
            ButtonState.Initial
        }
    }

    private fun deleteWaypointClicked() {
        navigationMap.selectedWaypoint?.also {
            clientSocket.send(CommandDeleteWaypoint(it.index))
        }
    }

    private fun scanShipClicked() {
        navigationMap.selectedContact?.also {
            if (it.scanLevel != ScanLevel.highest) {
                clientSocket.send(CommandScanShip(it.id))
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
                buttonState = ButtonState.Initial
            }
        }
    }

    private fun clearMapSelections() {
        navigationMap.selectedContact = null
        navigationMap.selectedWaypoint = null
    }

    private enum class ButtonState {
        Initial,
        AddWaypoint
    }
}
