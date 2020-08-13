package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.StationUiSwitcher
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

lateinit var commonShipUi: CommonShipUi
lateinit var joinUi: JoinUi
lateinit var destroyedUi: DestroyedUi
lateinit var helmUi: HelmUi
lateinit var weaponsUi: WeaponsUi
lateinit var navigationUi: NavigationUi
lateinit var engineeringUi: EngineeringUi
lateinit var mainScreenUi: MainScreenUi
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
    helmUi = HelmUi()
    weaponsUi = WeaponsUi()
    navigationUi = NavigationUi()
    engineeringUi = EngineeringUi()
    mainScreenUi = MainScreenUi()
    stationUiSwitcher = StationUiSwitcher(
        helmUi,
        weaponsUi,
        navigationUi,
        engineeringUi,
        mainScreenUi
    )

    commonShipUi = CommonShipUi().apply {
        hide()
    }

    window.requestAnimationFrame { step() }
    window.onresize = {
        helmUi.resize()
        weaponsUi.resize()
        navigationUi.resize()
        engineeringUi.resize()
        mainScreenUi.resize()
    }

    createSocket()

    document.onkeydown = { keyHandler(it) }
}

fun keyHandler(event: KeyboardEvent) {
    clientSocket.apply {
        when (event.code) {
            "KeyP" -> send(Command.CommandTogglePause)
            "KeyX" -> navigationUi.zoomIn()
            "KeyZ" -> navigationUi.zoomOut()
            "KeyR" -> {
                ClientState.toggleRotateScope()
            }
            "KeyC" -> mainScreenUi.cycleViewType()
            "KeyJ" -> send(Command.CommandStartJump)
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
    when (snapshot) {
        is SnapshotMessage.Helm -> {
            stationUiSwitcher.switchTo(Station.Helm)
            helmUi.draw(snapshot)
        }
        is SnapshotMessage.Weapons -> {
            stationUiSwitcher.switchTo(Station.Weapons)
            weaponsUi.draw(snapshot)
        }
        is SnapshotMessage.Navigation -> {
            stationUiSwitcher.switchTo(Station.Navigation)
            navigationUi.draw(snapshot)
        }
        is SnapshotMessage.Engineering -> {
            stationUiSwitcher.switchTo(Station.Engineering)
            engineeringUi.draw(snapshot)
        }
        is SnapshotMessage.MainScreen -> {
            stationUiSwitcher.switchTo(Station.MainScreen)
            mainScreenUi.draw(snapshot)
        }
    }
}
