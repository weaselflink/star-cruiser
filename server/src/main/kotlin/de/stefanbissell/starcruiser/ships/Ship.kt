package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamMessage
import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PhysicsEngine
import de.stefanbissell.starcruiser.PlayerShipMessage
import de.stefanbissell.starcruiser.PoweredSystem
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.ShipMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.toHeading
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.abs

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
    private val powerHandler = PowerHandler()
    private val beamHandlers = template.beams.map { BeamHandler(it) }
    private val shieldHandler = ShieldHandler(template.shield, powerHandler.boostLevel(PoweredSystem.Shields))
    private var scanHandler: ScanHandler? = null
    private var lockHandler: LockHandler? = null
    private var hull = template.hull
    private val jumpHandler = JumpHandler(template.jumpDrive)

    fun update(time: GameTime, physicsEngine: PhysicsEngine, shipProvider: (ObjectId) -> Ship?) {
        beamHandlers.forEach { it.update(time, shipProvider) }
        shieldHandler.update(time)
        jumpHandler.update(time)
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

    fun endUpdate(physicsEngine: PhysicsEngine): ShipUpdateResult {
        shieldHandler.endUpdate()
        val destroyed = hull <= 0.0
        if (!destroyed && jumpHandler.jumpComplete) {
            physicsEngine.jumpShip(id, jumpHandler.jumpDistance)
            jumpHandler.endJump()
        }
        return ShipUpdateResult(
            id = id,
            destroyed = destroyed
        )
    }

    fun targetDestroyed(shipId: ObjectId) {
        if (scanHandler?.targetId == shipId) {
            scanHandler = null
        }
        if (lockHandler?.targetId == shipId) {
            lockHandler = null
        }
    }

    private fun updateScan(time: GameTime) {
        scanHandler?.also {
            it.update(time)
            if (it.isComplete) {
                val scan = scans[it.targetId] ?: ScanLevel.None
                scans[it.targetId] = scan.next()
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
        if (!jumpHandler.jumping) {
            throttle = value.clamp(-100, 100)
        }
    }

    fun changeJumpDistance(value: Double) {
        if (!jumpHandler.jumping) {
            jumpHandler.changeJumpDistance(value)
        }
    }

    fun startJump() {
        if (jumpHandler.ready) {
            jumpHandler.startJump()
            throttle = 0
            rudder = 0
        }
    }

    fun changeRudder(value: Int) {
        if (!jumpHandler.jumping) {
            rudder = value.clamp(-100, 100)
        }
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
            scanHandler = ScanHandler(targetId, template.scanSpeed)
        }
    }

    fun lockTarget(targetId: ObjectId) {
        if (lockHandler?.targetId != targetId) {
            lockHandler = LockHandler(targetId, template.lockingSpeed)
        }
    }

    fun setShieldsUp(value: Boolean) {
        shieldHandler.setUp(value)
    }

    fun setPower(poweredSystem: PoweredSystem, power: Int) {
        powerHandler[poweredSystem] = power
    }

    fun takeDamage(amount: Double) {
        hull -= shieldHandler.takeDamageAndReportHullDamage(amount)
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
            shortRangeScopeRange = template.shortRangeScopeRange,
            waypoints = waypoints.map { it.toWaypointMessage(this) },
            scanProgress = scanHandler?.toMessage(),
            lockProgress = lockHandler?.toMessage() ?: LockStatus.NoLock,
            beams = beamHandlers.map { it.toMessage() },
            shield = shieldHandler.toMessage(),
            hull = hull,
            hullMax = template.hull,
            jumpDrive = jumpHandler.toMessage(),
            powerMessage = powerHandler.toMessage()
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
            shield = shieldHandler.toMessage(),
            jumpAnimation = jumpHandler.toMessage().animation
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
}

data class ShipUpdateResult(
    val id: ObjectId,
    val destroyed: Boolean
)
