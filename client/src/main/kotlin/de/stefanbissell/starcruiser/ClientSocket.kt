package de.stefanbissell.starcruiser

import kotlinx.browser.window
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

class ClientSocket {

    private var webSocket: WebSocket? = null
    var state: GameStateMessage? = null
        private set

    private val host
        get() = window.location.host
    private val protocol
        get() = if (window.location.protocol == "https:") {
            "wss:"
        } else {
            "ws:"
        }

    fun createSocket() {
        webSocket = WebSocket("$protocol//$host/ws/client").apply {
            onclose = {
                invalidateSocket()
            }
            onmessage = {
                it.parseAndAcknowledge()
            }
        }
    }

    private fun invalidateSocket() {
        webSocket = null
    }

    fun send(command: Command) {
        webSocket?.send(command.toJson())
    }

    private fun MessageEvent.parseAndAcknowledge() {
        state = parse().apply {
            send(Command.UpdateAcknowledge(counter = counter))
        }
    }

    private fun MessageEvent.parse() =
        GameStateMessage.parse(data.toString())
}
