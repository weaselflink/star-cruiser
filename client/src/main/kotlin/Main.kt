import de.bissell.starcruiser.Command
import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.ShipMessage
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

fun main() {
    window.onload = { init() }
}

fun init() {
    canvas = document.getElementById("canvas")!! as HTMLCanvasElement
    ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D

    canvas.resizeCanvasToDisplaySize()
    window.onresize = { canvas.resizeCanvasToDisplaySize() }
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
}

fun createSocket(): WebSocket? {
    val connectionInfos = document.getElementsByClassName("conn").asList()

    return WebSocket("$wsBaseUri/client").apply {
        clientSocket = this

        onopen = {
            connectionInfos.forEach {
                it.innerHTML = "connected"
            }
            Unit
        }
        onclose = {
            connectionInfos.forEach {
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
    val throttle: Long = state?.snapshot?.ship?.throttle?.toLong() ?: 0

    clientSocket?.apply {
        when (event.code) {
            "KeyP" -> send(Command.CommandTogglePause.toJson())
            "KeyW", "ArrowUp" -> send(Command.CommandChangeThrottle(throttle + 10).toJson())
            "KeyS", "ArrowDown" -> send(Command.CommandChangeThrottle(throttle - 10).toJson())
            "KeyA", "ArrowLeft" -> send(Command.CommandChangeRudder(-10).toJson())
            "KeyD", "ArrowRight" -> send(Command.CommandChangeRudder(10).toJson())
            else -> println("not bound: ${event.code}")
        }
    }
}

fun canvasClicked(event: MouseEvent) {
    val x = event.offsetX
    val y = event.offsetY

    if (x > 20.0 && x < 50.0 && y > canvas.height.toDouble() - 195.0 && y < canvas.height.toDouble() - 25.0) {
        val throttle = min(10.0, max(-10.0, -(y - canvas.height.toDouble() + 110.0) / 70.0 * 10.0)).toLong() * 10
        clientSocket?.send(Command.CommandChangeThrottle(throttle).toJson())
    }
}

fun HTMLCanvasElement.resizeCanvasToDisplaySize() {
    val windowWidth: Int = window.innerWidth
    val windowHeight: Int = window.innerHeight
    val dim: Int = min(window.innerWidth, window.innerHeight)

    if (width != dim || height != dim) {
        width = dim
        height = dim
    }

    style.left = ((windowWidth - dim) / 2).px
    style.top = ((windowHeight - dim) / 2).px
    style.width = dim.px
    style.height = dim.px
}

fun step() {
    state?.also {
        drawUi(it)
    }

    window.requestAnimationFrame { step() }
}

fun drawUi(stateCopy: GameStateMessage) {
    val ship = stateCopy.snapshot.ship

    updateInfo(ship)

    val joinUi = document.getElementById("join")!! as HTMLElement
    val helmUi = document.getElementById("helm")!! as HTMLElement

    if (ship != null) {
        joinUi.style.visibility = "hidden"
        helmUi.style.visibility = "visible"

        ctx.clearCanvas()
        ctx.drawCompass()

        ctx.drawThrottle(ship)
        ctx.drawShip(ship)
        ctx.drawHistory(ship)

        stateCopy.snapshot.contacts.forEach {
            ctx.drawContact(it)
        }
    } else {
        joinUi.style.visibility = "visible"
        helmUi.style.visibility = "hidden"

        updatePlayerShips(stateCopy)
    }
}

fun updateInfo(ship: ShipMessage?) {
    val headingElement = document.getElementById("heading")!!
    val velocityElement = document.getElementById("velocity")!!
    if (ship != null) {
        headingElement.innerHTML = ship.heading.toString()
        velocityElement.innerHTML = ship.velocity.toString()
    } else {
        headingElement.innerHTML = "unknown"
        velocityElement.innerHTML = "unknown"
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
    ellipse(
        canvas.width / 2.0, canvas.height / 2.0,
        dim / 2.0 - 15.0, dim / 2.0 - 15.0,
        0.0, 0.0, 2 * PI
    )
    fill()

    strokeStyle = "#666"
    lineWidth = 3.0
    lineCap = CanvasLineCap.ROUND
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
    lineWidth = 1.0
    lineCap = CanvasLineCap.BUTT
}

fun CanvasRenderingContext2D.drawThrottle(ship: ShipMessage) {
    resetTransform()

    fillStyle = "#111"
    beginPath()
    drawPill(20.0, canvas.height.toDouble() - 25.0, 30.0, 170.0)
    fill()

    strokeStyle = "#888"
    beginPath()
    drawPill(20.0, canvas.height.toDouble() - 25.0, 30.0, 170.0)
    stroke()

    fillStyle = "#999"
    beginPath()
    ellipse(
        35.0, canvas.height.toDouble() - 110.0 - ship.throttle / 100.0 * 70.0,
        15.0, 15.0, 0.0, 0.0, 2 * PI
    )
    fill()

    strokeStyle = "#666"
    beginPath()
    moveTo(24.0, canvas.height.toDouble() - 110.0)
    lineTo(46.0, canvas.height.toDouble() - 110.0)
    stroke()
}

fun CanvasRenderingContext2D.drawPill(x: Double, y: Double, width: Double, height: Double) {
    if (width > height) {
        val radius = height / 2.0
        moveTo(x + radius, y - radius * 2)
        lineTo(x + width - radius, y - radius * 2)
        arc(x + width - radius, y - radius, radius, -(PI / 2.0), PI / 2.0)
        lineTo(x + radius, y)
        arc(x + radius, y - radius, radius, PI / 2.0, -(PI / 2.0))
    } else {
        val radius = width / 2.0
        moveTo(x, y - radius)
        lineTo(x, y - height + radius)
        arc(x + radius, y - height + radius, radius, PI, 0.0)
        lineTo(x + radius * 2, y - radius)
        arc(x + radius, y - radius, radius, 0.0, PI)
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
    val rot = ship.rotation

    resetTransform()
    strokeStyle = "#1e90ff"
    beginPath()
    translate(canvas.width / 2.0, canvas.height / 2.0)
    drawShipSymbol(rot)
}

fun CanvasRenderingContext2D.drawContact(contact: ContactMessage) {
    val xPos = contact.relativePosition.x
    val yPos = contact.relativePosition.y
    val rot = contact.rotation

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
    val xPos = ship.position.x
    val yPos = -(ship.position.y)

    resetTransform()
    translate(canvas.width / 2.0, canvas.height / 2.0)
    for (point in ship.history) {
        val xp = point.second.x - xPos
        val yp = -(point.second.y) - yPos

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
