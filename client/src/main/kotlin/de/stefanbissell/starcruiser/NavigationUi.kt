package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandAddWaypoint
import de.stefanbissell.starcruiser.Command.CommandDeleteSelectedWaypoint
import de.stefanbissell.starcruiser.Command.CommandScanSelectedShip
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.CanvasSlider
import de.stefanbissell.starcruiser.components.MapClick
import de.stefanbissell.starcruiser.components.NavigationMap
import de.stefanbissell.starcruiser.components.ScanDisplay
import de.stefanbissell.starcruiser.components.SelectionDetails
import de.stefanbissell.starcruiser.components.StationUi
import de.stefanbissell.starcruiser.components.UiStyle
import org.w3c.dom.CanvasRenderingContext2D

class NavigationUi : StationUi(Station.Navigation) {

    private val navigationMap = NavigationMap(canvas) { handleMapClick(it) }
    private val selectionDetails = SelectionDetails(
        canvas = canvas,
        onScan = { scanShipClicked() },
        onDelete = { deleteWaypointClicked() }
    )
    private val zoomSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { vmin * 3 },
        yExpr = { height - vmin * 3 },
        widthExpr = { vmin * 50 },
        onChange = { navigationMap.changeZoom(it) },
        leftText = "Zoom"
    )
    private val addWaypointButton = CanvasButton(
        canvas = canvas,
        xExpr = { if (width > vmin * 136) vmin * 55 else vmin * 3 },
        yExpr = { if (width > vmin * 136) height - vmin * 3 else height - vmin * 15 },
        widthExpr = { vmin * 37 },
        onClick = { toggleAddWaypoint() },
        activated = { buttonState == ButtonState.AddWaypoint },
        initialText = "Add waypoint"
    )
    private val centerButton = CanvasButton(
        canvas = canvas,
        xExpr = {
            if (width > vmin * 161) {
                vmin * 94
            } else {
                vmin * 3
            }
        },
        yExpr = {
            when {
                width > vmin * 161 -> {
                    height - vmin * 3
                }
                width > vmin * 136 -> {
                    height - vmin * 15
                }
                else -> {
                    height - vmin * 27
                }
            }
        },
        widthExpr = { vmin * 23 },
        onClick = { navigationMap.centerOnShip() },
        initialText = "Center"
    )
    private val scanDisplay = ScanDisplay(canvas)

    private var buttonState: ButtonState = ButtonState.Initial

    init {
        addChildren(
            scanDisplay,
            zoomSlider,
            addWaypointButton,
            centerButton,
            selectionDetails,
            navigationMap
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

        selectionDetails.draw(snapshot.mapSelection)
        scanDisplay.draw(snapshot.ship.scanProgress)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Navigation) {
        transformReset()
        clear(UiStyle.mapBackgroundColor)

        navigationMap.draw(snapshot)
        drawZoom()
        addWaypointButton.draw()
        centerButton.draw()
    }

    private fun toggleAddWaypoint() {
        buttonState = if (buttonState != ButtonState.AddWaypoint) {
            ButtonState.AddWaypoint
        } else {
            ButtonState.Initial
        }
    }

    private fun deleteWaypointClicked() {
        ClientSocket.send(CommandDeleteSelectedWaypoint)
    }

    private fun scanShipClicked() {
        ClientSocket.send(CommandScanSelectedShip)
    }

    private fun drawZoom() =
        zoomSlider.draw(1.0 - navigationMap.scaleSetting / 6.0)

    private fun handleMapClick(mapClick: MapClick) {
        when (buttonState) {
            ButtonState.Initial -> handleSelectionClick(mapClick)
            ButtonState.AddWaypoint -> {
                ClientSocket.send(CommandAddWaypoint(mapClick.world))
                buttonState = ButtonState.Initial
            }
        }
    }

    private fun handleSelectionClick(mapClick: MapClick) {
        mapClick.contacts.firstOrNull()?.also {
            ClientSocket.send(Command.CommandMapSelectShip(it.id))
        } ?: mapClick.waypoints.firstOrNull()?.also {
            ClientSocket.send(Command.CommandMapSelectWaypoint(it.index))
        } ?: ClientSocket.send(Command.CommandMapClearSelection)
    }

    private enum class ButtonState {
        Initial,
        AddWaypoint
    }
}
