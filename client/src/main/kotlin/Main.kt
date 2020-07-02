import de.bissell.starcruiser.*
import org.w3c.dom.WebSocket
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
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null
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
        addExtraButtons(
            ExtraButton(
                ".toggleShields",
                weaponsUi::toggleShields,
                Station.Weapons
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
                ClientState.toggleRotateScope()
            }
            "KeyC" -> mainScreenUi.toggleTopView()
            "KeyJ" -> send(Command.CommandStartJump)
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

fun WebSocket?.send(command: Command) {
    this?.send(command.toJson())
}

interface StationUi {
    val station: Station
    fun show()
    fun hide()
}

class StationUiSwitcher(
    private val stations: List<StationUi>
) {

    init {
        stations.forEach { it.hide() }
    }

    fun switchTo(station: Station?) {
        stations.forEach {
            if (it.station == station) {
                it.show()
            } else {
                it.hide()
            }
        }
    }
}
