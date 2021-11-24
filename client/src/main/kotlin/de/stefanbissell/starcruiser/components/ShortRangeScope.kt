package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.ClientState
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScopeAsteroidMessage
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.SnapshotMessage.ShortRangeScopeStation
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.beamStyle
import de.stefanbissell.starcruiser.circle
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawAsteroidSymbol
import de.stefanbissell.starcruiser.drawLockMarker
import de.stefanbissell.starcruiser.drawPill
import de.stefanbissell.starcruiser.drawProjectileSymbol
import de.stefanbissell.starcruiser.drawVesselSymbol
import de.stefanbissell.starcruiser.enemyContactStyle
import de.stefanbissell.starcruiser.environmentContactStyle
import de.stefanbissell.starcruiser.friendlyContactStyle
import de.stefanbissell.starcruiser.historyStyle
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.lockMarkerStyle
import de.stefanbissell.starcruiser.neutralContactStyle
import de.stefanbissell.starcruiser.pad
import de.stefanbissell.starcruiser.shipStyle
import de.stefanbissell.starcruiser.toIntHeading
import de.stefanbissell.starcruiser.toRadians
import de.stefanbissell.starcruiser.translate
import de.stefanbissell.starcruiser.translateToCenter
import de.stefanbissell.starcruiser.tubeStyle
import de.stefanbissell.starcruiser.unknownContactStyle
import de.stefanbissell.starcruiser.wayPointStyle
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.MIDDLE
import org.w3c.dom.ROUND
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class ShortRangeScope(
    private val canvas: HTMLCanvasElement,
    private val showLocks: Boolean = false,
    private val scopeClickListener: ((ObjectId) -> Unit)? = null
) : PointerEventHandlerParent() {

    private val ctx = canvas.context2D
    private var dim = canvas.dimensions()

    private var scopeRadius = dim.vmin * 47
    private var lastSnapshot: ShortRangeScopeStation? = null

    init {
        addChildren(ScopePointerEventHandler())
    }

    fun draw(snapshot: ShortRangeScopeStation) {
        dim = canvas.dimensions()
        scopeRadius = dim.vmin * 47
        lastSnapshot = snapshot

        ctx.draw(snapshot)
    }

    private fun scopeClicked(pointerEvent: PointerEvent) {
        if (scopeClickListener == null) return

        val pointerOnScope = pointerEvent.adjustForScope()

        lastSnapshot?.contacts.orEmpty().map {
            it to (it.relativePosition.adjustForScope() - pointerOnScope).length()
        }.filter {
            it.second <= 20.0
        }.minByOrNull {
            it.second
        }?.also {
            scopeClickListener.invoke(it.first.id)
        }
    }

    private fun PointerEvent.adjustForScope(): Vector2 {
        val center = Vector2(dim.width * 0.5, dim.height * 0.5)
        return (point - center).rotate(-scopeRotation)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: ShortRangeScopeStation) {
        save()

        translateToCenter()
        rotate(scopeRotation)

        drawCompass()
        save()
        beginPath()
        circle(0.0, 0.0, scopeRadius)
        clip()

        drawHistory(snapshot)
        drawBeams(snapshot)
        drawTubes(snapshot)
        drawAsteroids(snapshot)
        drawWaypoints(snapshot)
        drawContacts(snapshot)
        drawLockedContact(snapshot)
        restore()
        drawScopeEdge()
        drawShip(snapshot)

        restore()

        drawHeading(snapshot)
    }

    private fun CanvasRenderingContext2D.drawCompass() {
        save()
        fillStyle = UiStyle.scopeBackgroundColor
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
        font = UiStyle.boldFont(textSize)
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

    private fun CanvasRenderingContext2D.drawHistory(snapshot: ShortRangeScopeStation) {
        save()
        historyStyle(dim)

        for (point in snapshot.shortRangeScope.history) {
            val posOnScope = point.adjustForScope()
            save()
            translate(posOnScope)
            beginPath()
            circle(0.0, 0.0, dim.min * 0.004)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawBeams(snapshot: ShortRangeScopeStation) {
        save()
        beamStyle(dim)
        rotate(-snapshot.shortRangeScope.rotation)

        for (beam in snapshot.shortRangeScope.beams) {
            val x = -beam.position.z.adjustForScope()
            val y = beam.position.x.adjustForScope()
            val left = -beam.leftArc.toRadians()
            val right = -beam.rightArc.toRadians()
            val minRange = beam.minRange.adjustForScope()
            val maxRange = beam.maxRange.adjustForScope()
            strokeStyle = when (beam.status) {
                is BeamStatus.Idle -> "#dc143c"
                is BeamStatus.Recharging -> "#750b20"
                is BeamStatus.Firing -> "#fb8532"
            }

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

    private fun CanvasRenderingContext2D.drawTubes(snapshot: ShortRangeScopeStation) {
        save()
        tubeStyle(dim)
        rotate(-snapshot.shortRangeScope.rotation)

        for (tube in snapshot.shortRangeScope.tubes) {
            val x = -tube.position.z.adjustForScope()
            val y = tube.position.x.adjustForScope()
            val range = 300.0.adjustForScope()

            save()
            translate(x, y)
            rotate(-tube.rotation)
            beginPath()
            moveTo(0.0, 0.0)
            lineTo(range, 0.0)
            stroke()
            restore()
        }

        restore()
    }

    private fun CanvasRenderingContext2D.drawAsteroids(snapshot: ShortRangeScopeStation) {
        save()
        environmentContactStyle(dim)

        snapshot.asteroids.forEach {
            drawAsteroid(it)
        }

        restore()
    }

    private fun CanvasRenderingContext2D.drawAsteroid(asteroid: ScopeAsteroidMessage) {
        val posOnScope = asteroid.relativePosition.adjustForScope()
        save()
        translate(posOnScope)
        drawAsteroidSymbol(asteroid.rotation, dim.vmin * 0.8 * asteroid.radius * 0.1)
        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(snapshot: ShortRangeScopeStation) {
        save()
        wayPointStyle(dim)

        val waypointConflictHandler = WaypointConflictHandler()
        for (waypoint in snapshot.shortRangeScope.waypoints) {
            val distance = waypoint.relativePosition.length()
            if (distance < shortRangeScopeRange * 0.9) {
                drawOnScopeWaypoint(waypoint)
            } else {
                drawOffScopeWaypoint(waypoint, waypointConflictHandler)
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
        waypoint: WaypointMessage,
        waypointConflictHandler: WaypointConflictHandler
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

        val offset = waypointConflictHandler.addWaypoint(angle)
        translate(0.0, -scopeRadius + dim.vmin * 7 + dim.vmin * 2.8 * offset)
        fillText(waypoint.name, 0.0, 0.0)

        restore()
    }

    private fun CanvasRenderingContext2D.drawContacts(snapshot: ShortRangeScopeStation) {
        snapshot.contacts.forEach {
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

    private fun CanvasRenderingContext2D.drawLockedContact(snapshot: ShortRangeScopeStation) {
        if (showLocks) {
            when (val lock = snapshot.shortRangeScope.lockProgress) {
                is LockStatus.InProgress -> drawLockProgress(snapshot, lock.targetId, lock.progress)
                is LockStatus.Locked -> drawLockProgress(snapshot, lock.targetId, 1.0)
                LockStatus.NoLock -> {}
            }
        }
    }

    private fun CanvasRenderingContext2D.drawLockProgress(
        snapshot: ShortRangeScopeStation,
        targetId: ObjectId,
        progress: Double
    ) {
        val contact = snapshot.contacts.firstOrNull {
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
            ContactType.Enemy -> enemyContactStyle(dim)
            ContactType.Neutral -> neutralContactStyle(dim)
            else -> unknownContactStyle(dim)
        }
        if (contact.shipType == ShipType.Vessel) {
            drawVesselSymbol(contact.rotation, dim.vmin * 0.8)
        } else {
            drawProjectileSymbol(contact.rotation, dim.vmin * 0.8)
        }
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

    private fun CanvasRenderingContext2D.drawShip(snapshot: ShortRangeScopeStation) {
        save()
        shipStyle(dim)
        drawVesselSymbol(snapshot.shortRangeScope.rotation, dim.vmin * 0.8)
        restore()
    }

    private fun CanvasRenderingContext2D.drawHeading(snapshot: ShortRangeScopeStation) {
        val centerX = dim.width / 2.0
        val centerY = dim.height / 2.0
        val width = dim.vmin * 12
        val height = dim.vmin * 5
        val textSize = (dim.vmin * 4).toInt()
        val heading = snapshot.shortRangeScope.rotation.toIntHeading()
        val headingText = heading.pad(3)

        save()

        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawPill(
            x = centerX - width / 2.0,
            y = centerY - scopeRadius + height / 2.0,
            width = width,
            height = height
        )
        fill()

        strokeStyle = "#666"
        lineWidth = dim.vmin * 0.4
        beginPath()
        drawPill(
            x = centerX - width / 2.0,
            y = centerY - scopeRadius + height / 2.0,
            width = width,
            height = height
        )
        stroke()

        fillStyle = "#aaa"
        font = UiStyle.boldFont(textSize)
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
        get() = if (ClientState.rotateScope) {
            (lastSnapshot?.shortRangeScope?.rotation ?: 0.0) - PI / 2.0
        } else {
            0.0
        }

    private val shortRangeScopeRange
        get() = lastSnapshot?.shortRangeScope?.shortRangeScopeRange ?: 100.0

    private class WaypointConflictHandler {
        private val added = mutableListOf<Pair<Double, Int>>()

        fun addWaypoint(angle: Double): Int {
            val offsets = added
                .filter { abs(it.first - angle) < PI / 20 }
                .map { it.second }
            val freeOffset = (0..offsets.size + 1).first { !offsets.contains(it) }
            added += angle to freeOffset
            return freeOffset
        }
    }

    private inner class ScopePointerEventHandler : PointerEventHandler {

        override fun handlePointerDown(pointerEvent: PointerEvent) {
            scopeClicked(pointerEvent)
        }
    }
}
