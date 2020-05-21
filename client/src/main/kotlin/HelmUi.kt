import de.bissell.starcruiser.*
import de.bissell.starcruiser.Station.Navigation
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.*

class HelmUi {

    private val root = document.getElementById("helm")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val fullScreenButton = root.querySelector(".fullscreen")!! as HTMLButtonElement
    private val toNavigationButton = root.querySelector(".switchToNavigation")!! as HTMLButtonElement
    private val throttleSlider = CanvasSlider(
        xExpr = { it.dim * 0.05 },
        yExpr = { it.dim - it.dim * 0.05 },
        widthExpr = { it.dim * 0.05 * 1.6 },
        heightExpr = { it.dim * 0.05 * 8.0 },
        lines = listOf(0.5)
    )
    private val rudderSlider = CanvasSlider(
        xExpr = { it.dim - it.dim * 0.05 - it.dim * 0.05 * 8.0 },
        yExpr = { it.dim - it.dim * 0.05 },
        widthExpr = { it.dim * 0.05 * 8.0 },
        heightExpr = { it.dim * 0.05 * 1.6 },
        lines = listOf(0.5)
    )

    private var dim = 100.0
    private var scopeRadius = 100.0
    private var rotateScope = false

    init {
        resize()
        canvas.onclick = { handleClick(it) }

        exitButton.onclick = { clientSocket.send(Command.CommandExitShip) }
        fullScreenButton.onclick = {
            val body = document.querySelector("body")!! as HTMLElement
            if (document.fullscreenElement == null) {
                body.requestFullscreen()
                fullScreenButton.innerText = "Windowed"
            } else {
                document.exitFullscreen()
                fullScreenButton.innerText = "Fullscreen"
            }
        }
        toNavigationButton.onclick = { clientSocket.send(Command.CommandChangeStation(Navigation)) }
    }

    fun resize() {
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
        scopeRadius = dim * 0.5 - dim * 0.03

        updateInfo(ship)

        with(ctx) {
            resetTransform()
            clear("#222")

            drawScope(stateCopy, ship)

            drawThrottle(ship)
            drawRudder(ship)
        }
    }

    private fun handleClick(event: MouseEvent) {
        if (throttleSlider.isClickInside(canvas, event)) {
            val throttle = min(10.0, max(-10.0, throttleSlider.clickValue(canvas, event) * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(Command.CommandChangeThrottle(throttle))
        }

        if (rudderSlider.isClickInside(canvas, event)) {
            val rudder = min(10.0, max(-10.0, rudderSlider.clickValue(canvas, event) * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(Command.CommandChangeRudder(rudder))
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

    private fun getScopeRotation(ship: ShipMessage) =
        if (rotateScope) {
            ship.rotation - PI / 2.0
        } else {
            0.0
        }

    private fun CanvasRenderingContext2D.drawScope(stateCopy: GameStateMessage, ship: ShipMessage) {
        save()

        translateToCenter()
        rotate(getScopeRotation(ship))

        drawCompass(ship)
        save()
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        clip()

        drawHistory(ship)
        drawWaypoints(ship)
        stateCopy.snapshot.contacts.forEach {
            ctx.drawContact(ship, it)
        }
        restore()
        drawScopeEdge()
        drawShip(ship)

        restore()
    }

    private fun CanvasRenderingContext2D.drawScopeEdge() {
        save()
        lineWidth = dim * 0.01
        strokeStyle = "#666"
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        stroke()
        restore()
    }

    private fun CanvasRenderingContext2D.drawCompass(ship: ShipMessage) {
        save()
        fillStyle = "#000"
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        fill()
        restore()

        save()
        val outer = scopeRadius - scopeRadius * 0.05
        val textPos = scopeRadius - scopeRadius * 0.16
        val textSize = (scopeRadius * 0.06).toInt()
        strokeStyle = "#222"
        fillStyle = "#222"
        lineWidth = dim * 0.006
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
        lineWidth = dim * 0.004
        for (i in 1..3) {
            val radius = (ship.shortRangeScopeRange / 4.0 * i).adjustForScope(ship)
            beginPath()
            circle(0.0, 0.0, radius)
            stroke()
        }
        restore()
    }

    private fun drawThrottle(ship: ShipMessage) =
        throttleSlider.draw(canvas, (ship.throttle + 100) / 200.0)

    private fun drawRudder(ship: ShipMessage) =
        rudderSlider.draw(canvas, (ship.rudder + 100) / 200.0)

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        val rot = ship.rotation

        save()
        shipStyle(dim)
        drawShipSymbol(rot, dim * 0.008)
        restore()
    }

    private fun CanvasRenderingContext2D.drawContact(ship: ShipMessage, contact: ContactMessage) {
        val rel = contact.relativePosition
        val rot = contact.rotation

        val posOnScope = rel.adjustForScope(ship)
        save()
        contactStyle(dim)

        translate(posOnScope)
        beginPath()
        drawShipSymbol(rot, dim * 0.008)

        rotate(-getScopeRotation(ship))
        translate(0.0, -dim * 0.02)
        fillText(contact.designation, 0.0, 0.0)
        restore()
    }

    private fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
        save()
        historyStyle(dim)

        for (point in ship.history) {
            val rel = (point.second - ship.position)
            val posOnScope = rel.adjustForScope(ship)
            save()
            translate(posOnScope)
            beginPath()
            circle(0.0, 0.0, dim * 0.004)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(ship: ShipMessage) {
        save()
        wayPointStyle(dim)

        for (waypoint in ship.waypoints) {
            val distance = waypoint.relativePosition.length()
            if (distance < ship.shortRangeScopeRange * 0.9) {
                drawOnScopeWaypoint(ship, waypoint)
            } else {
                drawOffScopeWaypoint(waypoint)
            }
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawOnScopeWaypoint(
        ship: ShipMessage,
        waypoint: WaypointMessage
    ) {
        val posOnScope = waypoint.relativePosition.adjustForScope(ship)
        save()

        translate(posOnScope)
        beginPath()
        circle(0.0, 0.0, dim * 0.008)
        stroke()

        rotate(-getScopeRotation(ship))
        translate(0.0, -dim * 0.02)
        fillText(waypoint.name, 0.0, 0.0)

        restore()
    }

    private fun CanvasRenderingContext2D.drawOffScopeWaypoint(
        waypoint: WaypointMessage
    ) {
        val rel = waypoint.relativePosition
        val angle = atan2(rel.y, rel.x)
        save()

        rotate(-angle + PI * 0.5)
        beginPath()
        moveTo(0.0, -scopeRadius + scopeRadius * 0.05)
        lineTo(scopeRadius * 0.03, -scopeRadius + scopeRadius * 0.08)
        lineTo(-scopeRadius * 0.03, -scopeRadius + scopeRadius * 0.08)
        closePath()
        stroke()

        translate(0.0, -scopeRadius + scopeRadius * 0.14)
        fillText(waypoint.name, 0.0, 0.0)

        restore()
    }

    private fun Double.adjustForScope(ship: ShipMessage) =
        (this * (scopeRadius / ship.shortRangeScopeRange))

    private fun Vector2.adjustForScope(ship: ShipMessage) =
        (this * (scopeRadius / ship.shortRangeScopeRange)).let { Vector2(it.x, -it.y) }
}
