package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.StationUiSwitcher
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

lateinit var joinUi: JoinUi
lateinit var destroyedUi: DestroyedUi
lateinit var stationUiSwitcher: StationUiSwitcher
val clientSocket = ClientSocket()

object ClientState {
    var rotateScope = false

    fun toggleRotateScope() {
        rotateScope = !rotateScope
    }
}

fun main() {
    window.onload = { init() }
}

fun init() {
    joinUi = JoinUi().apply { show() }

    destroyedUi = DestroyedUi().apply { hide() }
    stationUiSwitcher = StationUiSwitcher()

    window.requestAnimationFrame { step() }
    window.onresize = {
        stationUiSwitcher.resize()
    }

    clientSocket.createSocket()

    document.onkeydown = { keyHandler(it) }
}

fun keyHandler(event: KeyboardEvent) {
    when (event.code) {
        "KeyP" -> clientSocket.send(Command.CommandTogglePause)
        else -> println("not bound: ${event.code}")
    }
}

fun step() {
    clientSocket.state?.also {
        drawUi(it)
    }

    window.requestAnimationFrame { step() }
}

fun drawUi(stateCopy: GameStateMessage) {
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

fun drawShipUi(snapshot: SnapshotMessage.CrewSnapshot) {
    joinUi.hide()
    destroyedUi.hide()
    stationUiSwitcher.draw(snapshot)
}
