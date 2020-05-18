import de.bissell.starcruiser.ClientState.*
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import org.w3c.dom.WebSocket
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

lateinit var joinUi: JoinUi
lateinit var helmUi: HelmUi
lateinit var navigationUi: NavigationUi
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null

fun main() {
    window.onload = { init() }
}

fun init() {
    joinUi = JoinUi().apply { show() }
    helmUi = HelmUi().apply { hide() }
    navigationUi = NavigationUi().apply { hide() }

    window.requestAnimationFrame { step() }

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
    val throttle: Int = state?.snapshot?.ship?.throttle ?: 0
    val rudder: Int = state?.snapshot?.ship?.rudder ?: 0

    clientSocket?.apply {
        when (event.code) {
            "KeyP" -> send(Command.CommandTogglePause)
            "KeyW", "ArrowUp" -> send(Command.CommandChangeThrottle(throttle + 10))
            "KeyS", "ArrowDown" -> send(Command.CommandChangeThrottle(throttle - 10))
            "KeyA", "ArrowLeft" -> send(Command.CommandChangeRudder(rudder - 10))
            "KeyD", "ArrowRight" -> send(Command.CommandChangeRudder(rudder + 10))
            "KeyX" -> navigationUi.zoomIn()
            "KeyZ" -> navigationUi.zoomOut()
            "KeyR" -> helmUi.toggleRotateScope()
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
    val ship = stateCopy.snapshot.ship
    when (stateCopy.snapshot.clientState) {
        ShipSelection -> {
            helmUi.hide()
            navigationUi.hide()
            joinUi.show()
            joinUi.draw(stateCopy)
        }
        Helm -> {
            joinUi.hide()
            navigationUi.hide()
            helmUi.apply {
                show()
                draw(ship!!, stateCopy)
            }
        }
        Navigation -> {
            helmUi.hide()
            joinUi.hide()
            navigationUi.apply {
                show()
                draw(ship!!, stateCopy)
            }
        }
    }
}

fun WebSocket.send(command: Command) {
    send(command.toJson())
}
