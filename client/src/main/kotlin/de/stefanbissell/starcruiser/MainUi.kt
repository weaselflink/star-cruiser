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

        document.onkeydown = { handleKeyDown(it) }
        document.onkeyup = { handleKeyUp(it) }
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

    private fun handleKeyDown(event: KeyboardEvent) {
        when (event.code) {
            "KeyP" -> clientSocket.send(Command.CommandTogglePause)
            "KeyA" -> clientSocket.send(Command.CommandChangeRudder(100))
            "KeyD" -> clientSocket.send(Command.CommandChangeRudder(-100))
            "KeyW" -> clientSocket.send(Command.CommandChangeThrottle(100))
            "KeyS" -> clientSocket.send(Command.CommandChangeThrottle(-100))
            "KeyJ" -> clientSocket.send(Command.CommandStartJump)
            "KeyR" -> ClientState.toggleRotateScope()
            else -> println("not bound: ${event.code}")
        }
    }

    private fun handleKeyUp(event: KeyboardEvent) {
        when (event.code) {
            "KeyA" -> clientSocket.send(Command.CommandChangeRudder(0))
            "KeyD" -> clientSocket.send(Command.CommandChangeRudder(0))
            "KeyW" -> clientSocket.send(Command.CommandChangeThrottle(0))
            "KeyS" -> clientSocket.send(Command.CommandChangeThrottle(0))
        }
    }
}
