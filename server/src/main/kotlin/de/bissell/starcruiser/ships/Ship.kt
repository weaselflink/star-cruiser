package de.bissell.starcruiser.ships

import de.bissell.starcruiser.*
import java.util.*
import kotlin.math.abs

class Ship(
    val id: UUID = UUID.randomUUID(),
    val template: ShipTemplate = ShipTemplate(),
    val shortRangeScopeRange: Double = 400.0,
    private val designation: String = randomShipName(),
    var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    var rotation: Double = 90.0.toRadians(),
    private var throttle: Int = 0,
    private var rudder: Int = 0,
    val waypoints: MutableList<Waypoint> = mutableListOf()
) {

    private var thrust = 0.0
    private val history = mutableListOf<Pair<Double, Vector2>>()
    private val scans = mutableMapOf<UUID, ScanLevel>()

    fun update(time: GameTime, physicsEngine: PhysicsEngine) {
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
        println(position)
        (1..waypoints.size * 2 + 1).firstOrNull {
            waypoints.none { waypoint -> waypoint.index == it }
        }?.also {
            waypoints += Waypoint(it, position)
        }
    }

    fun deleteWaypoint(position: Vector2) {
        println(position)
        waypoints.map {
            (it.position - position).length() to it
        }.minBy {
            it.first
        }?.also {
            if (it.first < 100.0) {
                waypoints.remove(it.second)
            }
        }
    }

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id.toString(),
            name = designation,
            shipClass = template.className
        )

    fun toMessage() =
        ShipMessage(
            id = id.toString(),
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
            waypoints = waypoints.map { it.toWaypointMessage(this) }
        )

    fun toScopeContactMessage(relativeTo: Ship) =
        ScopeContactMessage(
            id = id.toString(),
            type = ContactType.Unknown,
            designation = designation,
            relativePosition = (position - relativeTo.position),
            rotation = rotation
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id.toString(),
            type = getContactType(relativeTo),
            scanLevel = relativeTo.getScanLevel(this),
            designation = designation,
            speed = speed,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            history = history.map { it.first to it.second }
        )

    private fun getScanLevel(ship: Ship) = scans[ship.id] ?: ScanLevel.None

    private fun getContactType(relativeTo: Ship) =
        if (relativeTo.getScanLevel(this) == ScanLevel.Faction) {
            ContactType.Friendly
        } else {
            ContactType.Unknown
        }
}

data class Waypoint(
    val index: Int,
    val position: Vector2
) {

    fun toWaypointMessage(relativeTo: Ship) =
        WaypointMessage(
            name = "WP$index",
            position = position,
            relativePosition = (position - relativeTo.position)
        )
}
