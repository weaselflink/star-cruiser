import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.RenderingContext
import org.w3c.dom.WebSocket
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min

const val wsBaseUri = "ws://127.0.0.1:35667/ws"

lateinit var canvas: HTMLCanvasElement
lateinit var ctx: RenderingContext
var clientSocket: WebSocket? = null

fun main() {
    window.onload = { init() }
}

fun init() {
    canvas = document.getElementById("canvas")!! as HTMLCanvasElement
    ctx = canvas.getContext(contextId = "2d")!!

    resizeCanvasToDisplaySize()

    window.onresize = { resizeCanvasToDisplaySize() }

    // createSocket("/client")

    document.onkeydown = { keyHandler(it) }
}

fun createSocket(uri: String) {
    val wsUri = wsBaseUri + uri;
    clientSocket = WebSocket(wsUri);

    val connectionInfo = document.getElementById("conn")!! as HTMLElement
    val socket = clientSocket
    if (socket != null) {
        socket.onopen = {
            connectionInfo.innerHTML = "connected"
            Unit
        }
        socket.onclose = {
            connectionInfo.innerHTML = "disconnected"
            println("Connection closed")
            clientSocket = null
            Unit
        }
    }
}

fun keyHandler(event: KeyboardEvent) {
    val socket = clientSocket
    println(event.code)
    if (socket != null) {
        when(event.code) {
            //"KeyP" -> socket.send(JSON.stringify(CommandTogglePause()));
            //"KeyW" -> socket.send(JSON.stringify(CommandChangeThrottle(10)));
            //"KeyS" -> socket.send(JSON.stringify(CommandChangeThrottle(-10)));
            //"KeyA" -> socket.send(JSON.stringify(CommandChangeRudder(-10)));
            //"KeyD" -> socket.send(JSON.stringify(CommandChangeRudder(10)));
        }
    }
}

fun resizeCanvasToDisplaySize() {
    val width: Int = window.innerWidth;
    val height: Int = window.innerHeight;
    val dim: Int = min(width, height);

    if (canvas.width != dim || canvas.height != dim) {
        canvas.width = dim;
        canvas.height = dim;
    }

    canvas.style.left = ((width - dim) / 2).px;
    canvas.style.top = ((height - dim) / 2).px;
    canvas.style.width = dim.px
    canvas.style.height = dim.px
}

val Int.px
    get() = "${this}px"
