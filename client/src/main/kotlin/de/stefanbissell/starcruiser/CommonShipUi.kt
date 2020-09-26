package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Station.Engineering
import de.stefanbissell.starcruiser.Station.Helm
import de.stefanbissell.starcruiser.Station.MainScreen
import de.stefanbissell.starcruiser.Station.Navigation
import de.stefanbissell.starcruiser.Station.Weapons
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

class CommonShipUi {

    private val root = document.getHtmlElementById("common-ship-ui")
    private val settingsButton: HTMLButtonElement = root.byQuery(".settings")
    private val exitButton: HTMLButtonElement = root.byQuery(".exit")
    private val fullScreenButton: HTMLButtonElement = root.byQuery(".fullscreen")
    private val pauseButton: HTMLButtonElement = root.byQuery(".pause")
    private val stationButtons = mapOf<Station, HTMLButtonElement>(
        Helm to root.byQuery(".switchToHelm"),
        Weapons to root.byQuery(".switchToWeapons"),
        Navigation to root.byQuery(".switchToNavigation"),
        Engineering to root.byQuery(".switchToEngineering"),
        MainScreen to root.byQuery(".switchToMainScreen")
    )
    private var showSettings = false
    private var currentStation: Station = Helm

    init {
        settingsButton.onclick = { toggleShowSettings() }
        exitButton.onclick = { clientSocket.send(Command.CommandExitShip) }
        fullScreenButton.onclick = {
            val body: HTMLElement = document.byQuery("body")
            if (document.fullscreenElement == null) {
                body.requestFullscreen()
                fullScreenButton.innerText = "Windowed"
            } else {
                document.exitFullscreen()
                fullScreenButton.innerText = "Fullscreen"
            }
        }
        pauseButton.onclick = {
            clientSocket.send(Command.CommandTogglePause)
        }

        stationButtons.forEach {
            it.value.onclick = { _ ->
                clientSocket.send(Command.CommandChangeStation(it.key))
            }
        }
    }

    fun show() {
        root.visibility = Visibility.visible
    }

    fun hide() {
        root.visibility = Visibility.hidden
        toggleShowSettings(false)
    }

    fun draw(snapshot: SnapshotMessage.CrewSnapshot) {
        val newStation = when (snapshot) {
            is SnapshotMessage.Weapons -> Weapons
            is SnapshotMessage.Navigation -> Navigation
            is SnapshotMessage.Engineering -> Engineering
            is SnapshotMessage.MainScreen -> MainScreen
            else -> Helm
        }

        if (newStation != currentStation) {
            stationButtons[currentStation]?.removeClass("current")
            stationButtons[newStation]?.addClass("current")
            currentStation = newStation
        }
    }

    private fun toggleShowSettings(value: Boolean = !showSettings) {
        showSettings = value
        if (showSettings) {
            settingsButton.innerHTML = "\u00d7"
            exitButton.display = Display.block
            fullScreenButton.display = Display.block
            pauseButton.display = Display.block
        } else {
            settingsButton.innerHTML = "\u2699"
            exitButton.display = Display.none
            fullScreenButton.display = Display.none
            pauseButton.display = Display.none
        }
    }
}
