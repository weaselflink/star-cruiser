package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.StationUiSwitcher
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

lateinit var commonShipUi: CommonShipUi
lateinit var joinUi: JoinUi
lateinit var destroyedUi: DestroyedUi
lateinit var stationUiSwitcher: StationUiSwitcher

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

    commonShipUi = CommonShipUi().apply {
        hide()
    }

    window.requestAnimationFrame { step() }
    window.onresize = {
        stationUiSwitcher.resize()
    }

    createSocket()

    document.onkeydown = { keyHandler(it) }
}

fun keyHandler(event: KeyboardEvent) {
    clientSocket.apply {
        when (event.code) {
            "KeyP" -> send(Command.CommandTogglePause)
            else -> println("not bound: ${event.code}")
        }
    }
}

fun step() {
    state?.also {
        drawUi(it)
    }

    window.requestAnimationFrame { step() }
}

fun drawUi(stateCopy: GameStateMessage) {
    when (val snapshot = stateCopy.snapshot) {
        is SnapshotMessage.ShipSelection -> {
            destroyedUi.hide()
            commonShipUi.hide()
            stationUiSwitcher.switchTo(null)
            joinUi.apply {
                show()
                draw(snapshot)
            }
        }
        is SnapshotMessage.ShipDestroyed -> {
            joinUi.hide()
            commonShipUi.hide()
            stationUiSwitcher.switchTo(null)
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
    commonShipUi.apply {
        show()
        draw(snapshot)
    }
    stationUiSwitcher.draw(snapshot)
}
