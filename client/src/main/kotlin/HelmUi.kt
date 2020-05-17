import de.bissell.starcruiser.*
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class HelmUi {

    private val root = document.getElementById("helm")!! as HTMLElement
    private val canvas = document.getElementById("canvas")!! as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D

    private var dim = 100.0
    private var scopeRadius = 100.0
    private var rotateScope = false

    init {
        resize()
        window.onresize = { resize() }
        canvas.onclick = { handleClick(it) }

        document.getElementsByClassName("exit").asList()
            .map {
                it as HTMLButtonElement
            }.forEach {
                it.onclick = { clientSocket?.send(Command.CommandExitShip.toJson()) }
            }
    }

    private fun resize() {
        val windowWidth: Int = window.innerWidth
        val windowHeight: Int = window.innerHeight
        val dim: Int = min(window.innerWidth, window.innerHeight)

        with (canvas) {
            if (width != dim || height != dim) {
                width = dim
                height = dim
            }

            style.left = ((windowWidth - dim) / 2).px
            style.top = ((windowHeight - dim) / 2).px
            style.width = dim.px
            style.height = dim.px
        }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun toggleRotateScope() {
        rotateScope = !rotateScope
    }

    fun draw(ship: ShipMessage, stateCopy: GameStateMessage) {
        dim = min(canvas.width, canvas.height).toDouble()
        scopeRadius = dim / 2.0 - dim / 10.0

        updateInfo(ship)

        with(ctx) {
            clearCanvas()

            drawThrottle(ship)
            drawRudder(ship)

            drawScope(stateCopy, ship)
        }
    }

    private fun handleClick(event: MouseEvent) {
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

    private fun updateInfo(ship: ShipMessage?) {
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

    private fun CanvasRenderingContext2D.clearCanvas() {
        resetTransform()
        fillStyle = "#222"
        fillRect(0.0, 0.0, dim, dim)
    }

    private fun CanvasRenderingContext2D.drawScope(stateCopy: GameStateMessage, ship: ShipMessage) {
        resetTransform()
        translateToCanvasCenter()
        if (rotateScope) {
            rotate(ship.rotation - PI / 2.0)
        }

        drawCompass(ship)
        drawHistory(ship)
        stateCopy.snapshot.contacts.forEach {
            ctx.drawContact(ship, it)
        }
        drawShip(ship)
    }

    private fun CanvasRenderingContext2D.drawCompass(ship: ShipMessage) {
        save()
        fillStyle = "#000"
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        fill()
        restore()

        save()
        lineWidth = 5.0
        strokeStyle = "#666"
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        stroke()
        restore()

        save()
        val outer = scopeRadius - scopeRadius * 0.05
        val textPos = scopeRadius - scopeRadius * 0.16
        val textSize = (scopeRadius * 0.06).toInt()
        strokeStyle = "#222"
        fillStyle = "#222"
        lineWidth = 3.0
        lineCap = CanvasLineCap.ROUND
        textAlign = CanvasTextAlign.CENTER
        font = "bold ${textSize.px} sans-serif"
        for (i in 0 until 36) {
            save()
            val angle = i * 10
            rotate(angle.toRadians())
            val inner = if (i % 3 == 0) {
                scopeRadius - scopeRadius * 0.1
            } else {
                scopeRadius - scopeRadius * 0.08
            }
            beginPath()
            moveTo(0.0, -inner)
            lineTo(0.0, -outer)
            stroke()
            if (i % 3 == 0) {
                fillText(angle.toString(), 0.0, -textPos)
            }
            restore()
        }
        restore()

        save()
        strokeStyle = "#222"
        lineWidth = 2.0
        for (i in 1..3) {
            val radius = (ship.shortRangeScopeRange / 4.0 * i).adjustForScope(ship)
            beginPath()
            circle(0.0, 0.0, radius)
            stroke()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawThrottle(ship: ShipMessage) {
        resetTransform()
        save()

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
        circle(
            bottomX + radius,
            bottomY - length / 2.0 - ship.throttle / 100.0 * (length / 2.0 - radius),
            radius * 0.8
        )
        fill()

        strokeStyle = "#666"
        beginPath()
        moveTo(bottomX + radius * 0.4, bottomY - length / 2.0)
        lineTo(bottomX + radius * 1.6, bottomY - length / 2.0)
        stroke()

        restore()
    }

    private fun CanvasRenderingContext2D.drawRudder(ship: ShipMessage) {
        resetTransform()
        save()

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
        circle(
            bottomX + length / 2.0 + ship.rudder / 100.0 * (length / 2.0 - radius),
            bottomY - radius,
            radius * 0.8
        )
        fill()

        strokeStyle = "#666"
        beginPath()
        moveTo(bottomX + length / 2.0, bottomY - radius * 0.4)
        lineTo(bottomX + length / 2.0, bottomY - radius * 1.6)
        stroke()

        restore()
    }

    private fun CanvasRenderingContext2D.drawPill(x: Double, y: Double, width: Double, height: Double) {
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

    private fun CanvasRenderingContext2D.drawShipSymbol(rot: Double) {
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

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        val rot = ship.rotation

        save()
        strokeStyle = "#1e90ff"
        drawShipSymbol(rot)
        restore()
    }

    private fun CanvasRenderingContext2D.drawContact(ship: ShipMessage, contact: ContactMessage) {
        val rel = contact.relativePosition
        val dist = sqrt(rel.x * rel.x + rel.y * rel.y)
        val rot = contact.rotation

        if (dist < ship.shortRangeScopeRange - 25.0) {
            val posOnScope = rel.adjustForScope(ship)
            save()
            strokeStyle = "#555"
            translate(posOnScope.x, posOnScope.y)
            beginPath()
            drawShipSymbol(rot)
            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
        save()
        fillStyle = "#222"

        for (point in ship.history) {
            val rel = (point.second - ship.position)
            val dist = sqrt(rel.x * rel.x + rel.y * rel.y)

            if (dist < ship.shortRangeScopeRange - 25.0) {
                val posOnScope = rel.adjustForScope(ship)
                save()
                translate(posOnScope.x, posOnScope.y)
                beginPath()
                circle(0.0, 0.0, 2.0)
                fill()
                restore()
            }
        }
        restore()
    }

    private fun CanvasRenderingContext2D.translateToCanvasCenter() {
        translate(canvas.width / 2.0, canvas.height / 2.0)
    }

    private fun CanvasRenderingContext2D.circle(x: Double, y: Double, radius: Double) =
        ellipse(x, y, radius, radius, 0.0, 0.0, PI * 2)

    private fun Double.adjustForScope(ship: ShipMessage) =
        (this * (scopeRadius / ship.shortRangeScopeRange))

    private fun Vector2.adjustForScope(ship: ShipMessage) =
        (this * (scopeRadius / ship.shortRangeScopeRange)).let { Vector2(it.x, -it.y) }
}

val Int.px
    get() = "${this}px"
