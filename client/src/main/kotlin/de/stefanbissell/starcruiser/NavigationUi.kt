package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandAddWaypoint
import de.stefanbissell.starcruiser.Command.CommandDeleteWaypoint
import de.stefanbissell.starcruiser.Command.CommandScanShip
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.CanvasSlider
import de.stefanbissell.starcruiser.components.MapClick
import de.stefanbissell.starcruiser.components.NavigationMap
import de.stefanbissell.starcruiser.components.SelectionDetails
import de.stefanbissell.starcruiser.components.UiStyle
import org.w3c.dom.CanvasRenderingContext2D

class NavigationUi : CanvasUi(Station.Navigation, "navigation-ui") {

    private val navigationMap = NavigationMap(canvas) { handleMapClick(it) }
    private val selectionDetails = SelectionDetails(
        canvas = canvas,
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
        initialText = "Add waypoint"
    )

    private var buttonState: ButtonState = ButtonState.Initial

    init {
        pointerEventDispatcher.addHandlers(
            zoomSlider,
            addWaypointButton,
            selectionDetails,
            navigationMap.MapPointerEventHandler()
        )
    }

    fun zoomIn() {
        navigationMap.zoomIn()
    }

    fun zoomOut() {
        navigationMap.zoomOut()
    }

    fun draw(snapshot: SnapshotMessage.Navigation) {
        ctx.draw(snapshot)

        selectionDetails.draw(navigationMap.selection)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Navigation) {
        transformReset()
        clear(UiStyle.mapBackgroundColor)

        navigationMap.draw(snapshot)
        drawZoom()
        addWaypointButton.draw()
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
