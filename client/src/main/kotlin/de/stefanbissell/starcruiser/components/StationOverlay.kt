package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
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

    private val buttonWidthExpr: CanvasDimensions.() -> Double = { 32.vmin }
    private val buttonHeightExpr: CanvasDimensions.() -> Double = { 8.vmin }
    private val buttonFullHeightExpr: CanvasDimensions.() -> Double = { buttonHeightExpr() + 1.vmin }
    private var currentStationButton: CanvasButton
    private var otherStationButtons: List<CanvasButton>
    private var exitButton: CanvasButton
    private var settingsButton: CanvasButton
    private var fullScreenButton: CanvasButton
    private var rotateScopeButton: CanvasButton
    private var pauseButton: CanvasButton

    private var currentStation = Station.Helm
    var visible = false

    init {
        verticalButtonGroup(
            canvas = document.canvas2d,
            rightXExpr = { width },
            topYExpr = { 0.vmin },
            buttonWidthExpr = { 32.vmin },
            buttonHeightExpr = { 8.vmin }
        ) {
            currentStationButton = addButton(
                onClick = { ClientState.toggleStationOverlay() },
                activated = { ClientState.showStationOverlay }
            )
            addGap()
            otherStationButtons = Station.values().map { station ->
                addButton(
                    onClick = { switchStation(station) },
                    activated = { station == currentStation },
                    enabled = { ClientState.showStationOverlay },
                    initialText = station.name
                )
            }
            addGap()
            exitButton = addButton(
                onClick = { ClientSocket.send(Command.CommandExitShip) },
                enabled = { ClientState.showStationOverlay },
                initialText = "Exit"
            )
        }

        verticalButtonGroup(
            canvas = document.canvas2d,
            leftXExpr = { 0.vmin },
            topYExpr = { 0.vmin },
            buttonWidthExpr = { 32.vmin },
            buttonHeightExpr = { 8.vmin }
        ) {
            settingsButton = addButton(
                onClick = { ClientState.toggleStationOverlay() },
                activated = { ClientState.showStationOverlay },
                initialText = "Settings"
            )
            addGap()
            fullScreenButton = addButton(
                onClick = { ClientState.toggleFullscreen() },
                activated = { ClientState.fullScreen },
                initialText = "Fullscreen"
            )
            rotateScopeButton = addButton(
                onClick = { ClientState.toggleRotateScope() },
                activated = { ClientState.rotateScope },
                initialText = "Rotate scope"
            )
            pauseButton = addButton(
                onClick = { ClientSocket.send(Command.CommandTogglePause) },
                initialText = "Pause"
            )
        }

        addChildren(
            currentStationButton,
            exitButton,
            settingsButton,
            fullScreenButton,
            rotateScopeButton,
            pauseButton
        )
        addChildren(otherStationButtons)
    }

    fun draw(snapshot: SnapshotMessage.CrewSnapshot) {
        currentStation = getNewStation(snapshot)

        if (currentStation != Station.MainScreen || ClientState.showStationOverlay) {
            if (ClientState.showStationOverlay) {
                currentStationButton.text = "Stations"
                drawStationButtons()
                drawSettingsButtons()
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
            xExpr = { width - (buttonWidthExpr() + 3.vmin) },
            yExpr = {
                2.vmin + buttonHeightExpr() + 2.vmin + (Station.values().size + 1) * buttonFullHeightExpr() + 1.vmin
            },
            widthExpr = { buttonWidthExpr() + 2.vmin },
            heightExpr = { (Station.values().size + 1) * buttonFullHeightExpr() + 2.vmin },
            radiusExpr = { 5.vmin }
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

        otherStationButtons.forEach(CanvasButton::draw)
        exitButton.draw()
    }

    private fun drawSettingsButtons() {
        val dim = calculateRect(
            canvas = document.canvas2d,
            xExpr = { 1.vmin },
            yExpr = {
                2.vmin + buttonHeightExpr() + 2.vmin + 3 * buttonFullHeightExpr()
            },
            widthExpr = { buttonWidthExpr() + 2.vmin },
            heightExpr = { 3 * buttonFullHeightExpr() + 1.vmin },
            radiusExpr = { 5.vmin }
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

        settingsButton.draw()
        fullScreenButton.draw()
        rotateScopeButton.draw()
        pauseButton.draw()
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
