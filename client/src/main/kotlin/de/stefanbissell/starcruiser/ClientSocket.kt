package de.stefanbissell.starcruiser

import kotlinx.browser.window
import org.w3c.dom.WebSocket

class ClientSocket {

    private var socket: WebSocket? = null
    var state: GameStateMessage? = null
        private set

    fun createSocket() {
        val protocol = if (window.location.protocol == "https:") {
            "wss:"
        } else {
            "ws:"
        }
        val host = window.location.host
        socket = WebSocket("$protocol//$host/ws/client").apply {
            onopen = {
                Unit
            }
            onclose = {
                socket = null
                Unit
            }
            onmessage = { event ->
                state = GameStateMessage.parse(event.data.toString()).apply {
                    send(Command.UpdateAcknowledge(counter = counter))
                }
                Unit
            }
        }
    }

    fun send(command: Command) {
        socket?.send(command.toJson())
    }
}
