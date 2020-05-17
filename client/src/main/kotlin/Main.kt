import de.bissell.starcruiser.Command
import de.bissell.starcruiser.GameStateMessage
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min

lateinit var helmUi: HelmUi
lateinit var canvas: HTMLCanvasElement
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null
var dim = 100.0
var rotateScope = false

fun main() {
    window.onload = { init() }
}

fun init() {
    helmUi = HelmUi()
    canvas = document.getElementById("canvas")!! as HTMLCanvasElement

    window.requestAnimationFrame { step() }

    canvas.onclick = { canvasClicked(it) }

    createSocket()

    document.onkeydown = { keyHandler(it) }

    document.getElementsByClassName("exit").asList()
        .map {
            it as HTMLButtonElement
        }.forEach {
            it.onclick = { clientSocket?.send(Command.CommandExitShip.toJson()) }
    }

    document.getElementsByClassName("spawn").asList()
        .map {
            it as HTMLButtonElement
        }.forEach {
            it.onclick = { clientSocket?.send(Command.CommandSpawnShip.toJson()) }
    }
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
            "KeyR" -> rotateScope = !rotateScope
            else -> println("not bound: ${event.code}")
        }
    }
}

fun canvasClicked(event: MouseEvent) {
    val x = event.offsetX
    val y = event.offsetY
    val length = dim / 20.0 * 8.0
    val throttleX = dim / 20.0
    val throttleY = dim - dim / 20.0
    val rudderX = dim - dim / 20.0 - length
    val rudderY = dim - dim / 20.0
    val radius = dim / 20.0 * 0.4

    if (x > throttleX && x < throttleX + radius * 2.0 && y > throttleY - length && y < throttleY) {
        val throttle = min(10, max(-10, (-(y - throttleY + length / 2.0) / (length / 2.0 - radius) * 10.0).toInt())) * 10
        clientSocket?.send(Command.CommandChangeThrottle(throttle).toJson())
    }

    if (x > rudderX && x < rudderX + length && y > rudderY - radius * 2.0 && y < rudderY) {
        val rudder = min(10, max(-10, ((x - rudderX - length / 2.0) / (length / 2.0 - radius) * 10.0).toInt())) * 10
        clientSocket?.send(Command.CommandChangeRudder(rudder).toJson())
    }
}

fun step() {
    state?.also {
        drawUi(it)
    }

    window.requestAnimationFrame { step() }
}

fun drawUi(stateCopy: GameStateMessage) {
    dim = min(canvas.width, canvas.height).toDouble()

    val ship = stateCopy.snapshot.ship

    val joinUi = document.getElementById("join")!! as HTMLElement

    if (ship != null) {
        joinUi.style.visibility = "hidden"
        helmUi.show()
        helmUi.draw(ship, stateCopy)
    } else {
        joinUi.style.visibility = "visible"
        helmUi.hide()

        updatePlayerShips(stateCopy)
    }
}

fun updatePlayerShips(stateCopy: GameStateMessage) {
    document.getElementsByClassName("playerShips").asList().forEach { playerShipsList ->
        val listElements = playerShipsList.getElementsByTagName("button")

        val max = max(stateCopy.snapshot.playerShips.size, listElements.length)

        for (index in 0 until max) {
            if (index < stateCopy.snapshot.playerShips.size) {
                val playerShip = stateCopy.snapshot.playerShips[index]
                val buttonText = playerShip.name + (playerShip.shipClass?.let { " ($it class)" } ?: "")
                if (index < listElements.length) {
                    listElements.item(index)!!.let {
                        it as HTMLElement
                    }.apply {
                        if (getAttribute("id") != playerShip.id) {
                            setAttribute("id", playerShip.id)
                            innerHTML = buttonText
                        }
                    }
                } else {
                    document.createElement("button").let {
                        it as HTMLElement
                    }.apply {
                        setAttribute("id", playerShip.id)
                        innerHTML = buttonText
                        onclick = { selectPlayerShip(it) }
                    }.also {
                        playerShipsList.appendChild(it)
                    }
                }
            } else {
                if (index < listElements.length) {
                    listElements.item(index)?.apply {
                        remove()
                    }
                }
            }
        }
    }
}

fun selectPlayerShip(event: MouseEvent) {
    clientSocket?.apply {
        val target = event.target as HTMLElement
        val shipId = target.attributes["id"]!!.value
        send(Command.CommandJoinShip(shipId = shipId).toJson())
    }
}

val Int.px
    get() = "${this}px"
