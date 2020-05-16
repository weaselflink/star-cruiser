import de.bissell.starcruiser.*
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.*

lateinit var canvas: HTMLCanvasElement
lateinit var ctx: CanvasRenderingContext2D
var clientSocket: WebSocket? = null
var state: GameStateMessage? = null
var scopeRadius = 100.0
var dim = 100.0

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
    dim = min(canvas.width, canvas.height).toDouble()
    scopeRadius = dim / 2.0 - dim / 10.0

    val ship = stateCopy.snapshot.ship

    updateInfo(ship)

    val joinUi = document.getElementById("join")!! as HTMLElement
    val helmUi = document.getElementById("helm")!! as HTMLElement

    if (ship != null) {
        joinUi.style.visibility = "hidden"
        helmUi.style.visibility = "visible"

        ctx.clearCanvas()
        ctx.drawCompass(ship)

        ctx.drawThrottle(ship)
        ctx.drawRudder(ship)
        stateCopy.snapshot.contacts.forEach {
            ctx.drawContact(ship, it)
        }
        ctx.drawHistory(ship)
        ctx.drawShip(ship)
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
        headingElement.innerHTML = ship.heading.format(1)
        velocityElement.innerHTML = ship.velocity.format(1)
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
    fillStyle = "#222"
    fillRect(0.0, 0.0, dim, dim)
}

fun CanvasRenderingContext2D.drawCompass(ship: ShipMessage) {
    resetTransform()
    translateToCanvasCenter()

    save()
    fillStyle = "#000"
    beginPath()
    ellipse(
        0.0, 0.0,
        scopeRadius, scopeRadius,
        0.0, 0.0, 2 * PI
    )
    fill()
    restore()

    save()
    lineWidth = 5.0
    strokeStyle = "#666"
    beginPath()
    ellipse(
        0.0, 0.0,
        scopeRadius, scopeRadius,
        0.0, 0.0, 2 * PI
    )
    stroke()
    restore()

    save()
    strokeStyle = "#666"
    lineWidth = 3.0
    lineCap = CanvasLineCap.ROUND
    for (i in 0 until 36) {
        val a = i * PI * 2 / 36
        val outer = scopeRadius - scopeRadius / 20
        val inner = if (i % 3 == 0) {
            scopeRadius - scopeRadius / 10
        } else {
            scopeRadius - scopeRadius / 15
        }
        beginPath()
        moveTo(sin(a) * inner, cos(a) * inner)
        lineTo(sin(a) * outer, cos(a) * outer)
        stroke()
    }
    restore()

    save()
    strokeStyle = "#222"
    lineWidth = 2.0
    for (i in 1..3) {
        val radius = (ship.shortRangeScopeRange / 4.0 * i).adjustForScope(ship)
        beginPath()
        ellipse(
            0.0, 0.0,
            radius, radius,
            0.0, 0.0, 2 * PI
        )
        stroke()
    }
    restore()
}

fun CanvasRenderingContext2D.drawThrottle(ship: ShipMessage) {
    resetTransform()

    val length = dim / 20.0 * 8.0
    val bottomX = dim / 20.0
    val bottomY = dim - dim / 20.0
    val radius = dim / 20.0 * 0.4

    lineWidth = 3.0
    fillStyle = "#111"
    beginPath()
    drawPill(bottomX, bottomY, radius * 2, length)
    fill()

    strokeStyle = "#888"
    beginPath()
    drawPill(bottomX, bottomY, radius * 2, length)
    stroke()

    fillStyle = "#999"
    beginPath()
    ellipse(
        bottomX + radius, bottomY - length / 2.0 - ship.throttle / 100.0 * (length / 2.0 - radius),
        radius * 0.8, radius * 0.8, 0.0, 0.0, 2 * PI
    )
    fill()

    strokeStyle = "#666"
    beginPath()
    moveTo(bottomX + radius * 0.4, bottomY - length / 2.0)
    lineTo(bottomX + radius * 1.6, bottomY - length / 2.0)
    stroke()
}

fun CanvasRenderingContext2D.drawRudder(ship: ShipMessage) {
    resetTransform()

    val length = dim / 20.0 * 8.0
    val bottomX = dim - dim / 20.0 - length
    val bottomY = dim - dim / 20.0
    val radius = dim / 20.0 * 0.4

    lineWidth = 3.0
    fillStyle = "#111"
    beginPath()
    drawPill(bottomX, bottomY, length, radius * 2)
    fill()

    strokeStyle = "#888"
    beginPath()
    drawPill(bottomX, bottomY, length, radius * 2)
    stroke()

    fillStyle = "#999"
    beginPath()
    ellipse(
        bottomX + length / 2.0 + ship.rudder / 100.0 * (length / 2.0 - radius), bottomY - radius,
        radius * 0.8, radius * 0.8, 0.0, 0.0, 2 * PI
    )
    fill()

    strokeStyle = "#666"
    beginPath()
    moveTo(bottomX + length / 2.0, bottomY - radius * 0.4)
    lineTo(bottomX + length / 2.0, bottomY - radius * 1.6)
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
    val baseUnit = dim / 80.0

    save()
    lineWidth = 3.0
    lineJoin = CanvasLineJoin.ROUND
    rotate(-rot)
    beginPath()
    moveTo(-baseUnit, -baseUnit)
    lineTo(baseUnit * 2, 0.0)
    lineTo(-baseUnit, baseUnit)
    lineTo(-baseUnit / 2, 0.0)
    closePath()
    stroke()
    restore()
}

fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
    val rot = ship.rotation

    resetTransform()
    strokeStyle = "#1e90ff"
    translateToCanvasCenter()
    drawShipSymbol(rot)
}

fun CanvasRenderingContext2D.drawContact(ship: ShipMessage, contact: ContactMessage) {
    val rel = contact.relativePosition
    val dist = sqrt(rel.x * rel.x + rel.y * rel.y)
    val rot = contact.rotation

    if (dist < ship.shortRangeScopeRange - 25.0) {
        val posOnScope = rel.adjustForScope(ship)
        resetTransform()
        strokeStyle = "#333"
        translateToCanvasCenter()
        translate(posOnScope.x, posOnScope.y)
        beginPath()
        drawShipSymbol(rot)
    }
}

fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
    resetTransform()
    fillStyle = "#666"
    translateToCanvasCenter()

    for (point in ship.history) {
        val rel = (point.second - ship.position)
        val dist = sqrt(rel.x * rel.x + rel.y * rel.y)

        if (dist < ship.shortRangeScopeRange - 25.0) {
            val posOnScope = rel.adjustForScope(ship)
            save()
            translate(posOnScope.x, posOnScope.y)
            beginPath()
            ellipse(0.0, 0.0, 2.0, 2.0, 0.0, 0.0, PI * 2)
            fill()
            restore()
        }
    }
}

private fun CanvasRenderingContext2D.translateToCanvasCenter() {
    translate(canvas.width / 2.0, canvas.height / 2.0)
}

fun Double.adjustForScope(ship: ShipMessage) =
    (this * (scopeRadius / ship.shortRangeScopeRange))

fun Vector2.adjustForScope(ship: ShipMessage) =
    (this * (scopeRadius / ship.shortRangeScopeRange)).let { Vector2(it.x, -it.y) }

val Int.px
    get() = "${this}px"
