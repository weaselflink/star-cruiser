package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.StationUiSwitcher
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

class MainUi {

    private val joinUi = JoinUi().apply { show() }
    private val destroyedUi = DestroyedUi().apply { hide() }
    private val stationUiSwitcher = StationUiSwitcher()

    init {
        window.requestAnimationFrame { step() }
        window.onresize = {
            stationUiSwitcher.resize()
        }

        document.onkeydown = { keyHandler(it) }
    }

    private fun step() {
        clientSocket.state?.also {
            drawUi(it)
        }

        window.requestAnimationFrame { step() }
    }

    private fun drawUi(stateCopy: GameStateMessage) {
        when (val snapshot = stateCopy.snapshot) {
            is SnapshotMessage.ShipSelection -> {
                destroyedUi.hide()
                stationUiSwitcher.hideAll()
                joinUi.apply {
                    show()
                    draw(snapshot)
                }
            }
            is SnapshotMessage.ShipDestroyed -> {
                joinUi.hide()
                stationUiSwitcher.hideAll()
                destroyedUi.show()
            }
            is SnapshotMessage.CrewSnapshot -> {
                drawShipUi(snapshot)
            }
        }
    }

    private fun drawShipUi(snapshot: SnapshotMessage.CrewSnapshot) {
        joinUi.hide()
        destroyedUi.hide()
        stationUiSwitcher.draw(snapshot)
    }

    private fun keyHandler(event: KeyboardEvent) {
        when (event.code) {
            "KeyP" -> clientSocket.send(Command.CommandTogglePause)
            else -> println("not bound: ${event.code}")
        }
    }
}
