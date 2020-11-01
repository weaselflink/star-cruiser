package de.stefanbissell.starcruiser

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

class CommonShipUi {

    private val root = document.getHtmlElementById("common-ship-ui")
    private val settingsButton: HTMLButtonElement = root.byQuery(".settings")
    private val exitButton: HTMLButtonElement = root.byQuery(".exit")
    private val fullScreenButton: HTMLButtonElement = root.byQuery(".fullscreen")
    private val rotateScopeButton: HTMLButtonElement = root.byQuery(".rotateScope")
    private val pauseButton: HTMLButtonElement = root.byQuery(".pause")
    private val settingsMenuButtons = listOf(
        exitButton,
        fullScreenButton,
        rotateScopeButton,
        pauseButton
    )
    private var showSettings = false

    init {
        settingsButton.onclick = { toggleShowSettings() }
        exitButton.onclick = { ClientSocket.send(Command.CommandExitShip) }
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
        rotateScopeButton.onclick = {
            ClientState.toggleRotateScope()
            if (ClientState.rotateScope) {
                rotateScopeButton.innerText = "Fixed scope"
            } else {
                rotateScopeButton.innerText = "Rotate scope"
            }
        }
        pauseButton.onclick = {
            ClientSocket.send(Command.CommandTogglePause)
        }
    }

    fun show() {
        root.visibility = Visibility.visible
    }

    fun hide() {
        root.visibility = Visibility.hidden
        toggleShowSettings(false)
    }

    private fun toggleShowSettings(value: Boolean = !showSettings) {
        showSettings = value
        if (showSettings) {
            settingsButton.innerHTML = "\u00d7"
            settingsMenuButtons.forEach {
                it.display = Display.block
            }
        } else {
            settingsButton.innerHTML = "\u2699"
            settingsMenuButtons.forEach {
                it.display = Display.none
            }
        }
    }
}
