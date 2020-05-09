
import kotlinx.serialization.ImplicitReflectionSerializer
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.*

const val wsBaseUri = "ws://127.0.0.1:35667/ws"

lateinit var canvas: HTMLCanvasElement
lateinit var ctx: CanvasRenderingContext2D
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null
var scopeRadius = 0

@ImplicitReflectionSerializer
fun main() {
    window.onload = { init() }
}

@ImplicitReflectionSerializer
fun init() {
    canvas = document.getElementById("canvas")!! as HTMLCanvasElement
    ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D

    resizeCanvasToDisplaySize()
    window.onresize = { resizeCanvasToDisplaySize() }
    window.requestAnimationFrame { step() }

    createSocket("/client")?.apply {
        onmessage = { event ->
            state = GameStateMessage.parse(event.data.toString())
            state?.also {
                send(UpdateAcknowledge(counter = it.counter.toInt()).toJson())
            }
            Unit
        }
    }

    document.onkeydown = { keyHandler(it) }
}

fun createSocket(uri: String): WebSocket? {
    val wsUri = wsBaseUri + uri
    clientSocket = WebSocket(wsUri)

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

    return clientSocket
}

@ImplicitReflectionSerializer
fun keyHandler(event: KeyboardEvent) {
    val socket = clientSocket
    if (socket != null) {
        when(event.code) {
            "KeyP" -> socket.send(CommandTogglePause.toJson())
            "KeyW" -> socket.send(CommandChangeThrottle(diff = 10).toJson())
            "KeyS" -> socket.send(CommandChangeThrottle(diff = -10).toJson())
            "KeyA" -> socket.send(CommandChangeRudder(diff = -10).toJson())
            "KeyD" -> socket.send(CommandChangeRudder(diff = 10).toJson())
        }
    }
}

fun resizeCanvasToDisplaySize() {
    val width: Int = window.innerWidth
    val height: Int = window.innerHeight
    val dim: Int = min(width, height)

    if (canvas.width != dim || canvas.height != dim) {
        canvas.width = dim
        canvas.height = dim
    }

    canvas.style.left = ((width - dim) / 2).px
    canvas.style.top = ((height - dim) / 2).px
    canvas.style.width = dim.px
    canvas.style.height = dim.px
}

@ImplicitReflectionSerializer
fun step() {
    state?.also {
        drawHelm(it)
    }

    window.requestAnimationFrame { step() }
}

@ImplicitReflectionSerializer
fun drawHelm(stateCopy: GameStateMessage) {

    val ship = stateCopy.snapshot.ship

    updateInfo(ship)
    updatePlayerShips(stateCopy)

    ctx.clearCanvas()
    ctx.drawCompass()

    if (ship != null) {
        ctx.drawShip(ship)
        ctx.drawHistory(ship)
    }

    stateCopy.snapshot.contacts.forEach {
        ctx.drawContact(it)
    }
}

fun updateInfo(ship: ShipMessage?) {
    val headingElement = document.getElementById("heading")!!
    val velocityElement = document.getElementById("velocity")!!
    if (ship != null) {
        headingElement.innerHTML = ship.heading
        velocityElement.innerHTML = ship.velocity
    } else {
        headingElement.innerHTML = "unknown"
        velocityElement.innerHTML = "unknown"
    }
}

@ImplicitReflectionSerializer
fun updatePlayerShips(stateCopy: GameStateMessage) {
    val playerShipsList = document.getElementById("playerShips")!!
    val listElements = playerShipsList.getElementsByTagName("li")

    val max = max(stateCopy.snapshot.playerShips.size, listElements.length)

    for (index in 0 until max) {
        if (index < stateCopy.snapshot.playerShips.size) {
            val playerShip = stateCopy.snapshot.playerShips[index]
            if (index < listElements.length) {
                val entry = listElements.item(index)!! as HTMLElement
                if (entry.getAttribute("id") != playerShip.id) {
                    entry.setAttribute("id", playerShip.id)
                    entry.innerHTML = playerShip.name
                }
            } else {
                val entry = document.createElement("li") as HTMLElement
                entry.setAttribute("id", playerShip.id)
                entry.innerHTML = playerShip.name
                entry.onclick = { selectPlayerShip(it) }
                playerShipsList.appendChild(entry)
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

@ImplicitReflectionSerializer
fun selectPlayerShip(event: MouseEvent) {
    val socket = clientSocket
    if (socket != null) {
        val target = event.target as HTMLElement
        val shipId = target.attributes["id"]!!.value
        socket.send(CommandJoinShip(shipId = shipId).toJson())
    }
}

fun CanvasRenderingContext2D.clearCanvas() {
    resetTransform()
    fillStyle = "#202020"
    fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
}

fun CanvasRenderingContext2D.drawCompass() {
    val dim = min(canvas.width, canvas.height)

    resetTransform()
    fillStyle = "#000"
    beginPath()
    ellipse(canvas.width / 2.0, canvas.height / 2.0,
        dim / 2.0 - 15.0, dim / 2.0 - 15.0,
        0.0, 0.0, 2 * PI)
    fill()

    strokeStyle = "#888"
    scopeRadius = dim / 2 - 20
    translate(canvas.width / 2.0, canvas.height / 2.0)
    for (i in 0 until 36) {
        val a = i * PI * 2 / 36
        val inner = if (i % 3 == 0) {
            scopeRadius - scopeRadius / 10
        } else {
            scopeRadius - scopeRadius / 20
        }
        beginPath()
        moveTo(sin(a) * inner, cos(a) * inner)
        lineTo(sin(a) * scopeRadius, cos(a) * scopeRadius)
        stroke()
    }
}

fun CanvasRenderingContext2D.drawShipSymbol(rot: Double) {
    rotate(-rot)
    moveTo(-5.0, -5.0)
    lineTo(10.0, 0.0)
    lineTo(-5.0, 5.0)
    lineTo(-2.0, 0.0)
    lineTo(-5.0, -5.0)
    stroke()
}

fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
    val rot = ship.rotation.toDouble()

    resetTransform()
    strokeStyle = "#1e90ff"
    beginPath()
    translate(canvas.width / 2.0, canvas.height / 2.0)
    drawShipSymbol(rot)
}

fun CanvasRenderingContext2D.drawContact(contact: ContactMessage) {
    val xPos = contact.relativePosition.x.toDouble()
    val yPos = contact.relativePosition.y.toDouble()
    val rot = contact.rotation.toDouble()

    val dist = sqrt(xPos * xPos + yPos * yPos)
    if (dist < scopeRadius - 10) {
        resetTransform()
        strokeStyle = "#333"
        beginPath()
        translate(canvas.width / 2 + xPos, canvas.height / 2 - yPos)
        drawShipSymbol(rot)
    }
}

fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
    val xPos = ship.position.x.toDouble()
    val yPos = -(ship.position.y.toDouble())

    resetTransform()
    translate(canvas.width / 2.0, canvas.height / 2.0)
    for (point in ship.history) {
        val xp = point.second.x.toDouble() - xPos
        val yp = -(point.second.y.toDouble()) - yPos

        val dist = sqrt(xp * xp + yp * yp)
        if (dist < scopeRadius - 10) {
            fillStyle = "#fff"
            beginPath()
            fillRect(xp, yp, 1.0, 1.0)
        }
    }
}

val Int.px
    get() = "${this}px"
