package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.ClientState
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Station
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

class StationOverlay : PointerEventHandlerParent() {

    private val canvas = document.querySelector(".canvas2d") as HTMLCanvasElement
    private val currentStationButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.width - it.vmin * 37 },
        yExpr = { it.vmin * 12 },
        widthExpr = { it.vmin * 35 },
        heightExpr = { it.vmin * 10 },
        onClick = { ClientState.toggleStationOverlay() },
        activated = { ClientState.showStationOverlay }
    )
    private val otherStationButtons = Station.values().mapIndexed { index, station ->
        CanvasButton(
            canvas = canvas,
            xExpr = { it.width - it.vmin * 37 },
            yExpr = { it.vmin * 27 + index * it.vmin * 12 },
            widthExpr = { it.vmin * 35 },
            heightExpr = { it.vmin * 10 },
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

        currentStationButton.text = currentStation.name
        currentStationButton.draw()

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
