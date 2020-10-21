package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.ClientState
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Station
import de.stefanbissell.starcruiser.canvas2d
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import kotlinx.browser.document

class StationOverlay : PointerEventHandlerParent() {

    private val currentStationButton = CanvasButton(
        canvas = document.canvas2d,
        xExpr = { width - vmin * 37 },
        yExpr = { vmin * 12 },
        widthExpr = { vmin * 35 },
        heightExpr = { vmin * 10 },
        onClick = { ClientState.toggleStationOverlay() },
        activated = { ClientState.showStationOverlay }
    )
    private val otherStationButtons = Station.values().mapIndexed { index, station ->
        CanvasButton(
            canvas = document.canvas2d,
            xExpr = { width - vmin * 37 },
            yExpr = { vmin * 27 + index * vmin * 12 },
            widthExpr = { vmin * 35 },
            heightExpr = { vmin * 10 },
            onClick = { switchStation(station) },
            activated = { station == currentStation },
            enabled = { ClientState.showStationOverlay },
            initialText = station.name
        )
    }

    private var currentStation = Station.Helm
    var visible = false

    init {
        addChildren(currentStationButton)
        addChildren(otherStationButtons)
    }

    fun draw(snapshot: SnapshotMessage.CrewSnapshot) {
        currentStation = getNewStation(snapshot)

        if (currentStation != Station.MainScreen || ClientState.showStationOverlay) {
            if (ClientState.showStationOverlay) {
                currentStationButton.text = "Stations"

                otherStationButtons.forEach {
                    it.draw()
                }
            } else {
                currentStationButton.text = currentStation.name
            }
            currentStationButton.draw()
        }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent) =
        visible && super.isInterestedIn(pointerEvent)

    private fun getNewStation(snapshot: SnapshotMessage.CrewSnapshot) =
        when (snapshot) {
            is SnapshotMessage.Weapons -> Station.Weapons
            is SnapshotMessage.Navigation -> Station.Navigation
            is SnapshotMessage.Engineering -> Station.Engineering
            is SnapshotMessage.MainScreen -> Station.MainScreen
            else -> Station.Helm
        }

    private fun switchStation(station: Station) {
        clientSocket.send(Command.CommandChangeStation(station))
    }
}
