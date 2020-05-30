import de.bissell.starcruiser.*
import org.w3c.dom.*
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.math.*

class HelmUi {

    private val root = document.getElementById("helm-ui")!! as HTMLElement
    private val canvas = root.querySelector("canvas") as HTMLCanvasElement
    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private val mouseEventDispatcher = MouseEventDispatcher(canvas)
    private val rotateScopeButton = document.querySelector(".rotateScope")!! as HTMLButtonElement
    private val throttleSlider = CanvasSlider(
        xExpr = { it.vmin * 5 },
        yExpr = { it.min - it.vmin * 5 },
        widthExpr = { it.vmin * 8 },
        heightExpr = { it.vmin * 40 },
        onChange = {
            val throttle = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(Command.CommandChangeThrottle(throttle))
        },
        lines = listOf(0.5),
        leftText = "Impulse"
    )
    private val rudderSlider = CanvasSlider(
        xExpr = { it.min - it.vmin * 5 - it.vmin * 40 },
        yExpr = { it.min - it.vmin * 5 },
        widthExpr = { it.vmin * 40 },
        heightExpr = { it.vmin * 8 },
        onChange = {
            val rudder = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(Command.CommandChangeRudder(rudder))
        },
        lines = listOf(0.5),
        leftText = "Rudder",
        reverseValue = true
    )

    private var dim = CanvasDimensions(100, 100)
    private var scopeRadius = 100.0
    private var rotateScope = false

    init {
        resize()
        mouseEventDispatcher.addHandler(throttleSlider)
        mouseEventDispatcher.addHandler(rudderSlider)
    }

    fun resize() {
        canvas.updateSize(square = true)
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun toggleRotateScope() {
        rotateScope = !rotateScope
        rotateScopeButton.removeClass("current")
        if (rotateScope) {
            rotateScopeButton.addClass("current")
        }
    }

    fun draw(snapshot: SnapshotMessage.Helm) {
        val ship = snapshot.ship
        dim = canvas.dimensions()
        scopeRadius = dim.min * 0.5 - dim.vmin * 3

        updateInfo(ship)

        with(ctx) {
            resetTransform()
            clear("#222")

            drawScope(snapshot, ship)
            drawHeading(ship)

            drawThrottle(ship)
            drawRudder(ship)
        }
    }

    private fun CanvasRenderingContext2D.drawHeading(ship: ShipMessage) {
        val centerX = dim.width / 2.0
        val centerY = dim.height / 2.0
        val width = dim.vmin * 12
        val height = dim.vmin * 5
        val textSize = (dim.vmin * 4).toInt()
        val headingText = ship.rotation.toHeading().roundToInt().pad(3)

        save()

        fillStyle = "#111"
        beginPath()
        drawPill(
            centerX - width / 2.0, centerY - scopeRadius + height / 2.0,
            width, height
        )
        fill()

        strokeStyle = "#666"
        lineWidth = dim.vmin * 0.4
        beginPath()
        drawPill(
            centerX - width / 2.0, centerY - scopeRadius + height / 2.0,
            width, height
        )
        stroke()

        fillStyle = "#aaa"
        font = "bold ${textSize.px} sans-serif"
        textAlign = CanvasTextAlign.CENTER
        textBaseline = CanvasTextBaseline.MIDDLE
        fillText(headingText, centerX, centerY - scopeRadius + dim.vmin * 0.35)

        restore()
    }

    private fun updateInfo(ship: ShipMessage) {
        document.getElementById("velocity")!!.innerHTML = ship.velocity.format(1)
    }

    private fun getScopeRotation(ship: ShipMessage) =
        if (rotateScope) {
            ship.rotation - PI / 2.0
        } else {
            0.0
        }

    private fun CanvasRenderingContext2D.drawScope(snapshot: SnapshotMessage.Helm, ship: ShipMessage) {
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
        snapshot.contacts.forEach {
            ctx.drawContact(ship, it)
        }
        restore()
        drawScopeEdge()
        drawShip(ship)

        restore()
    }

    private fun CanvasRenderingContext2D.drawScopeEdge() {
        save()
        lineWidth = dim.vmin
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
        lineWidth = dim.vmin * 0.6
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
        lineWidth = dim.vmin * 0.4
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
        drawShipSymbol(rot, dim.vmin * 0.8)
        restore()
    }

    private fun CanvasRenderingContext2D.drawContact(ship: ShipMessage, contact: ContactMessage) {

        val posOnScope = contact.relativePosition.adjustForScope(ship)
        save()
        contactStyle(dim)

        translate(posOnScope)
        beginPath()
        drawShipSymbol(contact.rotation, dim.vmin * 0.8)

        rotate(-getScopeRotation(ship))
        translate(0.0, -dim.vmin * 2)
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
            circle(0.0, 0.0, dim.min * 0.004)
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
        circle(0.0, 0.0, dim.vmin * 0.8)
        stroke()

        rotate(-getScopeRotation(ship))
        translate(0.0, -dim.vmin * 2)
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
