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
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 50 },
        heightExpr = { it.vmin * 10 },
        onChange = { navigationMap.changeZoom(it) },
        leftText = "Zoom"
    )
    private val addWaypointButton = CanvasButton(
        canvas = canvas,
        xExpr = { if (it.width > it.vmin * 136) it.vmin * 55 else it.vmin * 3 },
        yExpr = { if (it.width > it.vmin * 136) it.height - it.vmin * 3 else it.height - it.vmin * 15 },
        widthExpr = { it.vmin * 37 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleAddWaypoint() },
        activated = { buttonState == ButtonState.AddWaypoint },
        initialText = "Add waypoint"
    )
    private val centerButton = CanvasButton(
        canvas = canvas,
        xExpr = {
            if (it.width > it.vmin * 161) {
                it.vmin * 94
            } else {
                it.vmin * 3
            }
        },
        yExpr = {
            when {
                it.width > it.vmin * 161 -> {
                    it.height - it.vmin * 3
                }
                it.width > it.vmin * 136 -> {
                    it.height - it.vmin * 15
                }
                else -> {
                    it.height - it.vmin * 27
                }
            }
        },
        widthExpr = { it.vmin * 23 },
        heightExpr = { it.vmin * 10 },
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
        clientSocket.send(CommandDeleteSelectedWaypoint)
    }

    private fun scanShipClicked() {
        clientSocket.send(CommandScanSelectedShip)
    }

    private fun drawZoom() =
        zoomSlider.draw(1.0 - navigationMap.scaleSetting / 6.0)

    private fun handleMapClick(mapClick: MapClick) {
        when (buttonState) {
            ButtonState.Initial -> handleSelectionClick(mapClick)
            ButtonState.AddWaypoint -> {
                clientSocket.send(CommandAddWaypoint(mapClick.world))
                buttonState = ButtonState.Initial
            }
        }
    }

    private fun handleSelectionClick(mapClick: MapClick) {
        mapClick.contacts.firstOrNull()?.also {
            clientSocket.send(Command.CommandMapSelectShip(it.id))
        } ?: mapClick.waypoints.firstOrNull()?.also {
            clientSocket.send(Command.CommandMapSelectWaypoint(it.index))
        } ?: clientSocket.send(Command.CommandMapClearSelection)
    }

    private enum class ButtonState {
        Initial,
        AddWaypoint
    }
}
