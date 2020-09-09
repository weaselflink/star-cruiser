package de.stefanbissell.starcruiser

import kotlinx.browser.window
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

class ClientSocket {

    private var webSocket: WebSocket? = null
    var state: GameStateMessage? = null
        private set

    fun createSocket() {
        val protocol = if (window.location.protocol == "https:") {
            "wss:"
        } else {
            "ws:"
        }
        val host = window.location.host
        webSocket = WebSocket("$protocol//$host/ws/client").apply {
            onopen = {
                Unit
            }
            onclose = {
                webSocket = null
                Unit
            }
            onmessage = { event ->
                state = parse(event).apply {
                    send(Command.UpdateAcknowledge(counter = counter))
                }
                Unit
            }
        }
    }

    fun send(command: Command) {
        webSocket?.send(command.toJson())
    }

    private fun parse(event: MessageEvent) =
        GameStateMessage.parse(event.data.toString())
}
