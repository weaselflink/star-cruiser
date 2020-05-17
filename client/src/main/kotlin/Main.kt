import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import org.w3c.dom.WebSocket
import org.w3c.dom.asList
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

lateinit var joinUi: JoinUi
lateinit var helmUi: HelmUi
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null

fun main() {
    window.onload = { init() }
}

fun init() {
    joinUi = JoinUi().apply { show() }
    helmUi = HelmUi().apply { hide() }

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
            document.getElementsByClassName("conn").asList().forEach {
                it.innerHTML = "connected"
            }
            Unit
        }
        onclose = {
            document.getElementsByClassName("conn").asList().forEach {
                it.innerHTML = "disconnected"
            }
            println("Connection closed")
            clientSocket = null
            Unit
        }
        onmessage = { event ->
            GameStateMessage.parse(event.data.toString()).apply {
                state = this
            }.also {
                send(Command.UpdateAcknowledge(counter = it.counter).toJson())
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
            "KeyP" -> send(Command.CommandTogglePause.toJson())
            "KeyW", "ArrowUp" -> send(Command.CommandChangeThrottle(throttle + 10).toJson())
            "KeyS", "ArrowDown" -> send(Command.CommandChangeThrottle(throttle - 10).toJson())
            "KeyA", "ArrowLeft" -> send(Command.CommandChangeRudder(rudder - 10).toJson())
            "KeyD", "ArrowRight" -> send(Command.CommandChangeRudder(rudder + 10).toJson())
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

    if (ship != null) {
        joinUi.hide()
        helmUi.show()
        helmUi.draw(ship, stateCopy)
    } else {
        helmUi.hide()
        joinUi.show()
        joinUi.draw(stateCopy)
    }
}
