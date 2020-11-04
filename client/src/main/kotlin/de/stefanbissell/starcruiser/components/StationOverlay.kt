package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.ClientSocket
import de.stefanbissell.starcruiser.ClientState
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Station
import de.stefanbissell.starcruiser.canvas2d
import de.stefanbissell.starcruiser.components.ComponentDimensions.Companion.calculateRect
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import kotlinx.browser.document

class StationOverlay : PointerEventHandlerParent() {

    private val currentStationButton = CanvasButton(
        canvas = document.canvas2d,
        xExpr = { width - 37.vmin },
        yExpr = { 12.vmin },
        widthExpr = { 35.vmin },
        onClick = { ClientState.toggleStationOverlay() },
        activated = { ClientState.showStationOverlay }
    )
    private val otherStationButtons = Station.values().mapIndexed { index, station ->
        CanvasButton(
            canvas = document.canvas2d,
            xExpr = { width - 37.vmin },
            yExpr = { 27.vmin + index * 12.vmin },
            widthExpr = { 35.vmin },
            onClick = { switchStation(station) },
            activated = { station == currentStation },
            enabled = { ClientState.showStationOverlay },
            initialText = station.name
        )
    }
    private val exitButton = CanvasButton(
        canvas = document.canvas2d,
        xExpr = { width - 37.vmin },
        yExpr = { 30.vmin + Station.values().size * 12.vmin },
        widthExpr = { 35.vmin },
        onClick = { ClientSocket.send(Command.CommandExitShip) },
        enabled = { ClientState.showStationOverlay },
        initialText = "Exit"
    )

    private var currentStation = Station.Helm
    var visible = false

    init {
        addChildren(currentStationButton, exitButton)
        addChildren(otherStationButtons)
    }

    fun draw(snapshot: SnapshotMessage.CrewSnapshot) {
        currentStation = getNewStation(snapshot)

        if (currentStation != Station.MainScreen || ClientState.showStationOverlay) {
            if (ClientState.showStationOverlay) {
                currentStationButton.text = "Stations"
                drawStationButtons()
            } else {
                currentStationButton.text = currentStation.name
            }
            currentStationButton.draw()
        }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent) =
        visible && super.isInterestedIn(pointerEvent)

    private fun drawStationButtons() {
        val dim = calculateRect(
            canvas = document.canvas2d,
            xExpr = { width - 38.vmin },
            yExpr = { 31.vmin + otherStationButtons.size * 12.vmin },
            widthExpr = { 37.vmin },
            heightExpr = { (otherStationButtons.size + 1) * 12.vmin + 3.vmin },
            radiusExpr = { 6.vmin }
        )

        with(document.canvas2d.context2D) {
            save()

            lineWidth = dim.lineWidth
            fillStyle = UiStyle.buttonBackgroundColor
            beginPath()
            drawRect(dim)
            fill()

            restore()
        }

        otherStationButtons.forEach {
            it.draw()
        }
        exitButton.draw()
    }

    private fun getNewStation(snapshot: SnapshotMessage.CrewSnapshot) =
        when (snapshot) {
            is SnapshotMessage.Weapons -> Station.Weapons
            is SnapshotMessage.Navigation -> Station.Navigation
            is SnapshotMessage.Engineering -> Station.Engineering
            is SnapshotMessage.MainScreen -> Station.MainScreen
            else -> Station.Helm
        }

    private fun switchStation(station: Station) {
        ClientSocket.send(Command.CommandChangeStation(station))
    }
}
