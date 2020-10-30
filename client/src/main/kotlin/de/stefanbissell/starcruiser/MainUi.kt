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
        ClientSocket.state?.also {
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
            "KeyP" -> ClientSocket.send(Command.CommandTogglePause)
            "KeyA" -> ClientSocket.send(Command.CommandChangeRudder(100))
            "KeyD" -> ClientSocket.send(Command.CommandChangeRudder(-100))
            "KeyW" -> ClientSocket.send(Command.CommandChangeThrottle(100))
            "KeyS" -> ClientSocket.send(Command.CommandChangeThrottle(-100))
            "KeyJ" -> ClientSocket.send(Command.CommandStartJump)
            "KeyR" -> ClientState.toggleRotateScope()
            "Digit1" -> ClientSocket.send(Command.CommandChangeStation(Station.Helm))
            "Digit2" -> ClientSocket.send(Command.CommandChangeStation(Station.Weapons))
            "Digit3" -> ClientSocket.send(Command.CommandChangeStation(Station.Navigation))
            "Digit4" -> ClientSocket.send(Command.CommandChangeStation(Station.Engineering))
            "Digit5" -> ClientSocket.send(Command.CommandChangeStation(Station.MainScreen))
            "KeyX" -> ClientSocket.send(Command.CommandExitShip)
            else -> println("not bound: ${event.code}")
        }
    }

    private fun handleKeyUp(event: KeyboardEvent) {
        when (event.code) {
            "KeyA" -> ClientSocket.send(Command.CommandChangeRudder(0))
            "KeyD" -> ClientSocket.send(Command.CommandChangeRudder(0))
            "KeyW" -> ClientSocket.send(Command.CommandChangeThrottle(0))
            "KeyS" -> ClientSocket.send(Command.CommandChangeThrottle(0))
        }
    }
}
