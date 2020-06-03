package components

import beamStyle
import circle
import de.bissell.starcruiser.*
import de.bissell.starcruiser.SnapshotMessage.ShortRangeScopeStation
import dimensions
import drawLockMarker
import drawPill
import drawShipSymbol
import friendlyContactStyle
import historyStyle
import lockMarkerStyle
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import px
import shipStyle
import translate
import translateToCenter
import unknownContactStyle
import wayPointStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

class ShortRangeScope(
    private val canvas: HTMLCanvasElement,
    private val showLocks: Boolean = false,
    private val clickListener: ((ShipId) -> Unit)? = null
) {

    private val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
    private var dim = canvas.dimensions()
    private var scopeRadius = dim.vmin * 47
    private var ship: ShipMessage? = null
    private var contacts: List<ScopeContactMessage> = emptyList()
    var rotating = false
        private set

    init {
        canvas.onclick = { scopeClicked(it) }
    }

    fun toggleRotating() {
        rotating = !rotating
    }

    fun draw(snapshot: ShortRangeScopeStation) {
        dim = canvas.dimensions()
        scopeRadius = dim.vmin * 47
        ship = snapshot.ship
        contacts = snapshot.contacts

        ctx.draw(snapshot.ship)
    }

    private fun scopeClicked(mouseEvent: MouseEvent) {
        if (clickListener == null) return

        val mouseOnScope = mouseEvent.adjustForScope()

        contacts.map {
            it to (it.relativePosition.adjustForScope() - mouseOnScope).length()
        }.filter {
            it.second <= 20.0
        }.minBy {
            it.second
        }?.also {
            clickListener.invoke(it.first.id)
        }
    }

    private fun MouseEvent.adjustForScope(): Vector2 {
        val center = Vector2(dim.width * 0.5, dim.height * 0.5)
        return (Vector2(offsetX, offsetY) - center).rotate(-scopeRotation)
    }

    private fun CanvasRenderingContext2D.draw(ship: ShipMessage) {
        save()

        translateToCenter()
        rotate(scopeRotation)

        drawCompass()
        save()
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        clip()

        drawHistory(ship)
        drawBeams(ship)
        drawWaypoints(ship)
        drawContacts()
        drawLockedContact(ship)
        restore()
        drawScopeEdge()
        drawShip(ship)

        restore()

        drawHeading(ship)
    }

    private fun CanvasRenderingContext2D.drawCompass() {
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
            val radius = (shortRangeScopeRange / 4.0 * i).adjustForScope()
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
            val posOnScope = rel.adjustForScope()
            save()
            translate(posOnScope)
            beginPath()
            circle(0.0, 0.0, dim.min * 0.004)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawBeams(ship: ShipMessage) {
        save()
        beamStyle(dim)
        rotate(-ship.rotation)

        for (beam in ship.beams) {
            val x = -beam.position.z.adjustForScope()
            val y = beam.position.x.adjustForScope()
            val left = -beam.leftArc.toRadians()
            val right = -beam.rightArc.toRadians()
            val minRange = beam.minRange.adjustForScope()
            val maxRange = beam.maxRange.adjustForScope()

            save()
            translate(x, y)
            beginPath()
            circle(0.0, 0.0, maxRange, left, right)
            circle(0.0, 0.0, minRange, right, left, true)
            closePath()
            stroke()
            restore()
        }

        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(ship: ShipMessage) {
        save()
        wayPointStyle(dim)

        for (waypoint in ship.waypoints) {
            val distance = waypoint.relativePosition.length()
            if (distance < shortRangeScopeRange * 0.9) {
                drawOnScopeWaypoint(waypoint)
            } else {
                drawOffScopeWaypoint(waypoint)
            }
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawOnScopeWaypoint(
        waypoint: WaypointMessage
    ) {
        val posOnScope = waypoint.relativePosition.adjustForScope()
        save()

        translate(posOnScope)
        beginPath()
        circle(0.0, 0.0, dim.vmin * 0.8)
        stroke()

        rotate(-scopeRotation)
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

    private fun CanvasRenderingContext2D.drawContacts() {
        contacts.forEach {
            if (!showLocks || !it.locked) {
                drawContact(it)
            }
        }
    }

    private fun CanvasRenderingContext2D.drawContact(contact: ScopeContactMessage) {
        val posOnScope = contact.relativePosition.adjustForScope()
        save()
        translate(posOnScope)
        drawContactShipSymbol(contact)

        rotate(-scopeRotation)
        translate(0.0, -dim.vmin * 3)
        fillText(contact.designation, 0.0, 0.0)
        restore()
    }

    private fun CanvasRenderingContext2D.drawLockedContact(ship: ShipMessage) {
        if (showLocks) {
            when (val lock = ship.lockProgress) {
                is LockInProgress -> drawLockProgress(lock.targetId, lock.progress)
                is Locked -> drawLockProgress(lock.targetId, 1.0)
            }
        }
    }

    private fun CanvasRenderingContext2D.drawLockProgress(
        targetId: ShipId,
        progress: Double
    ) {
        val contact = contacts.firstOrNull {
            it.id == targetId
        } ?: return
        val posOnScope = contact.relativePosition.adjustForScope()
        val scale = 2.0 - progress

        save()
        translate(posOnScope)
        drawContactShipSymbol(contact)

        lockMarkerStyle(dim)
        rotate(-scopeRotation)
        drawLockMarker(dim.vmin * 3.2 * scale)
        if (progress >= 1.0) {
            save()
            rotate(PI * 0.25)
            drawLockMarker(dim.vmin * 3.2 * scale)
            restore()
        }
        translate(0.0, -dim.vmin * 4)
        fillText(contact.designation, 0.0, 0.0)
        restore()
    }

    private fun CanvasRenderingContext2D.drawContactShipSymbol(contact: ScopeContactMessage) {
        when (contact.type) {
            ContactType.Friendly -> friendlyContactStyle(dim)
            else -> unknownContactStyle(dim)
        }
        drawShipSymbol(contact.rotation, dim.vmin * 0.8)
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

    private fun Double.adjustForScope() =
        (this * (scopeRadius / shortRangeScopeRange))

    private fun Vector2.adjustForScope() =
        (this * (scopeRadius / shortRangeScopeRange)).let { Vector2(it.x, -it.y) }

    private val scopeRotation
        get() = if (rotating) {
            (ship?.rotation ?: 0.0) - PI / 2.0
        } else {
            0.0
        }

    private val shortRangeScopeRange
        get() = ship?.shortRangeScopeRange ?: 100.0
}
