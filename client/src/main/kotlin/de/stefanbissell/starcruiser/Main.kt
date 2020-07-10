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
    mainScreenUi = MainScreenUi()
    stationUiSwitcher = StationUiSwitcher(
        listOf(
            helmUi,
            weaponsUi,
            navigationUi,
            mainScreenUi
        )
    )

    commonShipUi = CommonShipUi().apply {
        hide()
    }

    window.requestAnimationFrame { step() }
    window.onresize = {
        helmUi.resize()
        weaponsUi.resize()
        navigationUi.resize()
        mainScreenUi.resize()
    }

    createSocket()

    document.onkeydown = { keyHandler(it) }
}

fun keyHandler(event: KeyboardEvent) {
    val throttle: Int = state.currentShip()?.throttle ?: 0
    val rudder: Int = state.currentShip()?.rudder ?: 0

    clientSocket.apply {
        when (event.code) {
            "KeyP" -> send(Command.CommandTogglePause)
            "KeyW", "ArrowUp" -> send(Command.CommandChangeThrottle(throttle + 10))
            "KeyS", "ArrowDown" -> send(Command.CommandChangeThrottle(throttle - 10))
            "KeyA", "ArrowLeft" -> send(Command.CommandChangeRudder(rudder + 10))
            "KeyD", "ArrowRight" -> send(Command.CommandChangeRudder(rudder - 10))
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

fun GameStateMessage?.currentShip(): ShipMessage? {
    return this?.snapshot?.let {
        when (it) {
            is SnapshotMessage.ShipSnapshot -> it.ship
            else -> null
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
        is SnapshotMessage.ShipSnapshot -> {
            drawShipUi(snapshot)
        }
    }
}

fun drawShipUi(snapshot: SnapshotMessage.ShipSnapshot) {
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
        is SnapshotMessage.MainScreen -> {
            stationUiSwitcher.switchTo(Station.MainScreen)
            mainScreenUi.draw(snapshot)
        }
    }
}
