package de.bissell.starcruiser.ships

import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.ContactType
import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.PhysicsEngine
import de.bissell.starcruiser.PlayerShipMessage
import de.bissell.starcruiser.ScanLevel
import de.bissell.starcruiser.ScanProgress
import de.bissell.starcruiser.ScopeContactMessage
import de.bissell.starcruiser.ShipId
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.WaypointMessage
import de.bissell.starcruiser.clamp
import de.bissell.starcruiser.randomShipName
import de.bissell.starcruiser.toHeading
import de.bissell.starcruiser.toRadians
import java.util.UUID
import kotlin.math.abs

class Ship(
    val id: ShipId = ShipId.random(),
    val template: ShipTemplate = ShipTemplate(),
    val shortRangeScopeRange: Double = 400.0,
    private val designation: String = randomShipName(),
    var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    var rotation: Double = 90.0.toRadians(),
    private var throttle: Int = 0,
    private var rudder: Int = 0
) {

    private var thrust = 0.0
    private val waypoints: MutableList<Waypoint> = mutableListOf()
    private val history = mutableListOf<Pair<Double, Vector2>>()
    private val scans = mutableMapOf<ShipId, ScanLevel>()
    private var scanHandler: ScanHandler? = null

    fun update(time: GameTime, physicsEngine: PhysicsEngine) {
        updateScan(time)
        updateThrust(time)
        val effectiveThrust = if (thrust < 0) {
            thrust * template.reverseThrustFactor
        } else {
            thrust * template.aheadThrustFactor
        }
        val effectiveRudder = rudder * template.rudderFactor
        physicsEngine.updateShip(id, effectiveThrust, effectiveRudder)

        physicsEngine.getShipStats(id)?.let {
            position = it.position
            speed = it.speed
            rotation = it.rotation
        }

        updateHistory(time)
    }

    private fun updateScan(time: GameTime) {
        scanHandler?.also {
            it.update(time)
            if (it.isComplete) {
                val scan = scans[scanHandler!!.targetId] ?: ScanLevel.None
                scans[scanHandler!!.targetId] = scan.next()
                scanHandler = null
            }
        }
    }

    private fun updateThrust(time: GameTime) {
        val responsiveness = template.throttleResponsiveness
        val diff = if (throttle > thrust) responsiveness else if (throttle < thrust) -responsiveness else 0.0
        thrust = (thrust + diff * time.delta).clamp(-100.0, 100.0)
    }

    private fun updateHistory(time: GameTime) {
        if (history.isEmpty()) {
            history.add(Pair(time.current, position))
        } else {
            if (abs(history.last().first - time.current) > 1.0) {
                history.add(Pair(time.current, position))
            }
            if (history.size > 10) {
                history.removeAt(0)
            }
        }
    }

    fun changeThrottle(value: Int) {
        throttle = value.clamp(-100, 100)
    }

    fun changeRudder(value: Int) {
        rudder = value.clamp(-100, 100)
    }

    fun addWaypoint(position: Vector2) {
        (1..waypoints.size * 2 + 1).firstOrNull {
            waypoints.none { waypoint -> waypoint.index == it }
        }?.also {
            waypoints += Waypoint(it, position)
        }
    }

    fun deleteWaypoint(index: Int) {
        waypoints.removeIf { it.index == index }
    }

    fun startScan(targetId: ShipId) {
        if (scanHandler == null && canIncreaseScanLevel(targetId)) {
            scanHandler = ScanHandler(targetId)
        }
    }

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id,
            name = designation,
            shipClass = template.className
        )

    fun toMessage() =
        ShipMessage(
            id = id,
            designation = designation,
            shipClass = template.className,
            speed = speed,
            position = position,
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            throttle = throttle,
            thrust = thrust,
            rudder = rudder,
            history = history.map { it.first to it.second },
            shortRangeScopeRange = shortRangeScopeRange,
            waypoints = waypoints.map { it.toWaypointMessage(this) },
            scanProgress = scanHandler?.toMessage()
        )

    fun toScopeContactMessage(relativeTo: Ship) =
        ScopeContactMessage(
            id = id,
            type = getContactType(relativeTo),
            designation = designation,
            relativePosition = (position - relativeTo.position),
            rotation = rotation
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id,
            type = getContactType(relativeTo),
            scanLevel = relativeTo.getScanLevel(id),
            designation = designation,
            speed = speed,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            history = history.map { it.first to it.second }
        )

    private fun canIncreaseScanLevel(targetId: ShipId) = getScanLevel(targetId).let { it != it.next() }

    private fun getScanLevel(targetId: ShipId) = scans[targetId] ?: ScanLevel.None

    private fun getContactType(relativeTo: Ship) =
        if (relativeTo.getScanLevel(id) == ScanLevel.Faction) {
            ContactType.Friendly
        } else {
            ContactType.Unknown
        }

    private inner class ScanHandler(
        val targetId: ShipId,
        private var progress: Double = 0.0
    ) {

        var isComplete: Boolean = false
            get() = progress >= 1.0
            private set

        fun update(time: GameTime) {
            progress += time.delta * template.scanSpeed
        }

        fun toMessage() =
            ScanProgress(
                targetId = targetId,
                progress = progress
            )
    }

    private inner class Waypoint(
        val index: Int,
        val position: Vector2
    ) {

        fun toWaypointMessage(relativeTo: Ship) =
            WaypointMessage(
                index = index,
                name = "WP$index",
                position = position,
                relativePosition = (position - relativeTo.position)
            )
    }

    companion object {
        private fun ShipId.Companion.random() = ShipId(UUID.randomUUID().toString())
    }
}
