import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import org.w3c.dom.WebSocket
import kotlin.browser.window

var clientSocket: WebSocket? = null
var state: GameStateMessage? = null

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

fun WebSocket?.send(command: Command) {
    this?.send(command.toJson())
}