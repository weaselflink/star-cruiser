package components

import circle
import contactStyle
import de.bissell.starcruiser.*
import dimensions
import drawPill
import drawShipSymbol
import historyStyle
import org.w3c.dom.*
import px
import shipStyle
import translate
import translateToCenter
import wayPointStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

class ShortRangeScope(
    private val canvas: HTMLCanvasElement
) {

    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private var dim = canvas.dimensions()
    private var scopeRadius = dim.vmin * 47
    var rotating = false
        private set

    fun toggleRotating() {
        rotating = !rotating
    }

    fun draw(snapshot: SnapshotMessage.Helm) {
        dim = canvas.dimensions()
        scopeRadius = dim.vmin * 47

        ctx.draw(snapshot, snapshot.ship)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Helm, ship: ShipMessage) {
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
        drawContacts(snapshot, ship)
        restore()
        drawScopeEdge()
        drawShip(ship)

        restore()

        drawHeading(ship)
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

    private fun CanvasRenderingContext2D.drawContacts(
        snapshot: SnapshotMessage.Helm,
        ship: ShipMessage
    ) {
        snapshot.contacts.forEach {
            drawContact(ship, it)
        }
    }

    private fun CanvasRenderingContext2D.drawContact(ship: ShipMessage, contact: ScopeContactMessage) {
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

    private fun CanvasRenderingContext2D.drawScopeEdge() {
        save()
        lineWidth = dim.vmin
        strokeStyle = "#666"
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        stroke()
        restore()
    }

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        val rot = ship.rotation

        save()
        shipStyle(dim)
        drawShipSymbol(rot, dim.vmin * 0.8)
        restore()
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

    private fun Double.adjustForScope(ship: ShipMessage) =
        (this * (scopeRadius / ship.shortRangeScopeRange))

    private fun Vector2.adjustForScope(ship: ShipMessage) =
        (this * (scopeRadius / ship.shortRangeScopeRange)).let { Vector2(it.x, -it.y) }

    private fun getScopeRotation(ship: ShipMessage) =
        if (rotating) {
            ship.rotation - PI / 2.0
        } else {
            0.0
        }
}
