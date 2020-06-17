package de.bissell.starcruiser.ships

import de.bissell.starcruiser.BeamMessage
import de.bissell.starcruiser.BeamStatus
import de.bissell.starcruiser.ContactMessage
import de.bissell.starcruiser.ContactType
import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.LockStatus
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.PhysicsEngine
import de.bissell.starcruiser.PlayerShipMessage
import de.bissell.starcruiser.ScanLevel
import de.bissell.starcruiser.ScanProgress
import de.bissell.starcruiser.ScopeContactMessage
import de.bissell.starcruiser.ShieldMessage
import de.bissell.starcruiser.ShipMessage
import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.WaypointMessage
import de.bissell.starcruiser.clamp
import de.bissell.starcruiser.randomShipName
import de.bissell.starcruiser.toHeading
import de.bissell.starcruiser.toRadians
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Ship(
    val id: ObjectId = ObjectId.random(),
    val template: ShipTemplate = ShipTemplate(),
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
    private val scans = mutableMapOf<ObjectId, ScanLevel>()
    private val beamHandlers = template.beams.map { BeamHandler(it) }
    private val shieldHandler = ShieldHandler()
    private var scanHandler: ScanHandler? = null
    private var lockHandler: LockHandler? = null

    fun update(time: GameTime, physicsEngine: PhysicsEngine, shipProvider: (ObjectId) -> Ship?) {
        beamHandlers.forEach { it.update(time, shipProvider) }
        shieldHandler.update(time)
        updateScan(time)
        updateLock(time)
        updateThrust(time)
        val effectiveThrust = if (thrust < 0) {
            thrust * template.reverseThrustFactor
        } else {
            thrust * template.aheadThrustFactor
        }
        val effectiveRudder = rudder * template.rudderFactor
        physicsEngine.updateShip(id, effectiveThrust, effectiveRudder)

        physicsEngine.getBodyParameters(id)?.let {
            position = it.position
            speed = it.speed
            rotation = it.rotation
        }

        updateHistory(time)
    }

    fun endUpdate() {
        shieldHandler.endUpdate()
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

    private fun updateLock(time: GameTime) {
        lockHandler?.also {
            if (!it.isComplete) {
                it.update(time)
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
        waypoints.sortBy { it.index }
    }

    fun deleteWaypoint(index: Int) {
        waypoints.removeIf { it.index == index }
    }

    fun startScan(targetId: ObjectId) {
        if (scanHandler == null && canIncreaseScanLevel(targetId)) {
            scanHandler = ScanHandler(targetId)
        }
    }

    fun lockTarget(targetId: ObjectId) {
        if (lockHandler?.targetId != targetId) {
            lockHandler = LockHandler(targetId)
        }
    }

    fun setShieldsUp(value: Boolean) {
        shieldHandler.setUp(value)
    }

    fun takeDamage(amount: Double) = shieldHandler.takeDamage(amount)

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
            shortRangeScopeRange = template.shortRangeScopeRange,
            waypoints = waypoints.map { it.toWaypointMessage(this) },
            scanProgress = scanHandler?.toMessage(),
            lockProgress = lockHandler?.toMessage() ?: LockStatus.NoLock,
            beams = beamHandlers.map { it.toMessage() },
            shield = shieldHandler.toMessage()
        )

    fun toScopeContactMessage(relativeTo: Ship) =
        ScopeContactMessage(
            id = id,
            type = getContactType(relativeTo),
            designation = designation,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            locked = relativeTo.isLocking(id)
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id,
            type = getContactType(relativeTo),
            scanLevel = relativeTo.getScanLevel(id),
            designation = designation,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            bearing = (position - relativeTo.position).angle().toHeading(),
            beams = beamHandlers.map { it.toMessage() },
            shield = shieldHandler.toMessage()
        )

    private fun canIncreaseScanLevel(targetId: ObjectId) = getScanLevel(targetId).let { it != it.next() }

    private fun getScanLevel(targetId: ObjectId) = scans[targetId] ?: ScanLevel.None

    private fun getContactType(relativeTo: Ship) =
        if (relativeTo.getScanLevel(id) == ScanLevel.Faction) {
            ContactType.Friendly
        } else {
            ContactType.Unknown
        }

    private fun isLocking(targetId: ObjectId) =
        if (lockHandler != null) {
            lockHandler?.targetId == targetId
        } else {
            false
        }

    private inner class ScanHandler(
        val targetId: ObjectId,
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

    private inner class LockHandler(
        val targetId: ObjectId,
        private var progress: Double = 0.0
    ) {

        var isComplete: Boolean = false
            get() = progress >= 1.0
            private set

        fun update(time: GameTime) {
            progress += time.delta * template.lockingSpeed
        }

        fun toMessage(): LockStatus =
            if (isComplete) {
                LockStatus.Locked(targetId)
            } else {
                LockStatus.InProgress(targetId, progress)
            }
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
                relativePosition = (position - relativeTo.position),
                bearing = (position - relativeTo.position).angle().toHeading()
            )
    }

    private inner class BeamHandler(
        val beamWeapon: BeamWeapon
    ) {

        private var status: BeamStatus = BeamStatus.Idle

        fun update(time: GameTime, shipProvider: (ObjectId) -> Ship?) {
            when (val current = status) {
                is BeamStatus.Idle -> if (isLockedTargetInRange(shipProvider)) {
                    status = BeamStatus.Firing()
                }
                is BeamStatus.Recharging -> {
                    status = current.update(time.delta * beamWeapon.rechargeSpeed).let {
                        if (it.progress >= 1.0) {
                            if (isLockedTargetInRange(shipProvider)) {
                                BeamStatus.Firing()
                            } else {
                                BeamStatus.Idle
                            }
                        } else it
                    }
                }
                is BeamStatus.Firing -> {
                    status = current.update(time.delta * beamWeapon.firingSpeed).let {
                        if (it.progress >= 1.0) BeamStatus.Recharging() else it
                    }
                }
            }
            if (status is BeamStatus.Firing) {
                getLockedTarget(shipProvider)?.takeDamage(time.delta)
            }
        }

        fun toMessage() =
            BeamMessage(
                position = beamWeapon.position,
                minRange = beamWeapon.range.first.toDouble(),
                maxRange = beamWeapon.range.last.toDouble(),
                leftArc = beamWeapon.leftArc.toDouble(),
                rightArc = beamWeapon.rightArc.toDouble(),
                status = status,
                targetId = getLockedTargetId()
            )

        private fun getLockedTargetId() =
            if (lockHandler?.isComplete == true) lockHandler?.targetId else null

        private fun getLockedTarget(shipProvider: (ObjectId) -> Ship?) =
            getLockedTargetId()?.let { shipProvider(it) }

        private fun isLockedTargetInRange(shipProvider: (ObjectId) -> Ship?) =
            getLockedTarget(shipProvider)
                ?.toScopeContactMessage(this@Ship)
                ?.relativePosition
                ?.rotate(-this@Ship.rotation)
                ?.let { beamWeapon.isInRange(it) }
                ?: false
    }

    private inner class ShieldHandler {

        private var up: Boolean = true
        private var damageSinceLastUpdate: Double = 0.0
        private var activated: Boolean = false
        private var currentStrength: Double = template.shield.strength

        fun update(time: GameTime) {
            currentStrength = min(
                template.shield.strength,
                currentStrength + template.shield.rechargeSpeed * time.delta
            )
        }

        fun endUpdate() {
            activated = damageSinceLastUpdate > 0.0
            damageSinceLastUpdate = 0.0
        }

        fun takeDamage(amount: Double) {
            if (up) {
                damageSinceLastUpdate += amount
                currentStrength = max(
                    0.0,
                    currentStrength - amount
                )
            }
        }

        fun setUp(value: Boolean) {
            up = value
        }

        fun toMessage() =
            ShieldMessage(
                radius = template.shieldRadius,
                up = up,
                activated = activated,
                strength = currentStrength,
                max = template.shield.strength
            )
    }
}
