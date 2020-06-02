import de.bissell.starcruiser.*
import org.w3c.dom.WebSocket
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

lateinit var commonShipUi: CommonShipUi
lateinit var joinUi: JoinUi
lateinit var helmUi: HelmUi
lateinit var weaponsUi: WeaponsUi
lateinit var navigationUi: NavigationUi
lateinit var mainScreenUi: MainScreenUi
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null

fun main() {
    window.onload = { init() }
}

fun init() {
    joinUi = JoinUi().apply { show() }
    helmUi = HelmUi().apply { hide() }
    weaponsUi = WeaponsUi().apply { hide() }
    navigationUi = NavigationUi().apply { hide() }
    mainScreenUi = MainScreenUi().apply { hide() }
    commonShipUi = CommonShipUi().apply {
        hide()
        addExtraButtons(
            ExtraButton(
                ".rotateScope",
                {
                    helmUi.toggleRotateScope()
                    weaponsUi.toggleRotateScope()
                },
                Station.Helm,
                Station.Weapons
            ),
            ExtraButton(
                ".lockTarget",
                weaponsUi::lockTarget,
                Station.Weapons
            ),
            ExtraButton(
                ".addWaypoint",
                navigationUi::addWayPointClicked,
                Station.Navigation
            ),
            ExtraButton(
                ".deleteWaypoint",
                navigationUi::deleteWayPointClicked,
                Station.Navigation
            ),
            ExtraButton(
                ".scanShip",
                navigationUi::scanShipClicked,
                Station.Navigation
            ),
            ExtraButton(
                ".topView",
                mainScreenUi::toggleTopView,
                Station.MainScreen
            )
        )
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

fun createSocket(): WebSocket? {
    val protocol = if (window.location.protocol == "https:") {
        "wss:"
    } else {
        "ws:"
    }
    val host = window.location.host
    return WebSocket("$protocol//$host/ws/client").apply {
        clientSocket = this

        onopen = {
            Unit
        }
        onclose = {
            clientSocket = null
            Unit
        }
        onmessage = { event ->
            GameStateMessage.parse(event.data.toString()).apply {
                state = this
            }.also {
                send(Command.UpdateAcknowledge(counter = it.counter))
            }
            Unit
        }
    }
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
                helmUi.toggleRotateScope()
                weaponsUi.toggleRotateScope()
            }
            "KeyC" -> mainScreenUi.toggleTopView()
            else -> println("not bound: ${event.code}")
        }
    }
}

fun GameStateMessage?.currentShip() : ShipMessage? {
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
            commonShipUi.hide()
            helmUi.hide()
            weaponsUi.hide()
            navigationUi.hide()
            mainScreenUi.hide()
            joinUi.apply {
                show()
                draw(snapshot)
            }
        }
        is SnapshotMessage.ShipSnapshot -> {
            drawShipUi(snapshot)
        }
    }
}

fun drawShipUi(snapshot: SnapshotMessage.ShipSnapshot) {
    commonShipUi.apply {
        show()
        draw(snapshot)
    }
    when (snapshot) {
        is SnapshotMessage.Helm -> {
            joinUi.hide()
            weaponsUi.hide()
            navigationUi.hide()
            mainScreenUi.hide()
            helmUi.apply {
                show()
                draw(snapshot)
            }
        }
        is SnapshotMessage.Weapons -> {
            joinUi.hide()
            helmUi.hide()
            navigationUi.hide()
            mainScreenUi.hide()
            weaponsUi.apply {
                show()
                draw(snapshot)
            }
        }
        is SnapshotMessage.Navigation -> {
            helmUi.hide()
            weaponsUi.hide()
            joinUi.hide()
            mainScreenUi.hide()
            navigationUi.apply {
                show()
                draw(snapshot)
            }
        }
        is SnapshotMessage.MainScreen -> {
            helmUi.hide()
            weaponsUi.hide()
            joinUi.hide()
            navigationUi.hide()
            mainScreenUi.apply {
                show()
                draw(snapshot)
            }
        }
    }
}

fun WebSocket?.send(command: Command) {
    this?.send(command.toJson())
}
