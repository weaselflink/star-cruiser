package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.MainScreenView
import de.stefanbissell.starcruiser.MapContactMessage
import de.stefanbissell.starcruiser.MapSelectionMessage
import de.stefanbissell.starcruiser.NavigationShipMessage
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PlayerShipMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.PoweredSystemType.Impulse
import de.stefanbissell.starcruiser.PoweredSystemType.Jump
import de.stefanbissell.starcruiser.PoweredSystemType.Maneuver
import de.stefanbissell.starcruiser.PoweredSystemType.Sensors
import de.stefanbissell.starcruiser.PoweredSystemType.Shields
import de.stefanbissell.starcruiser.PoweredSystemType.Weapons
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.ShipMessage
import de.stefanbissell.starcruiser.ShortRangeScopeMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.toHeading
import de.stefanbissell.starcruiser.toRadians
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.abs
import kotlin.math.max

class PlayerShip(
    override val id: ObjectId = ObjectId.random(),
    override val template: ShipTemplate = cruiserTemplate,
    override val designation: String = randomShipName(),
    override var position: Vector2 = Vector2(),
    override var rotation: Double = 90.0.toRadians()
) : Ship {

    private val waypoints: MutableList<Waypoint> = mutableListOf()
    private val history = mutableListOf<Pair<Double, Vector2>>()
    private val scans = mutableMapOf<ObjectId, ScanLevel>()
    private val throttleHandler = ThrottleHandler(template)
    private val powerHandler = PowerHandler(template)
    private val beamHandlers = template.beams.map { BeamHandler(it, this) }
    private val shieldHandler = ShieldHandler(template.shield)
    private var mapSelection: MapSelection = MapSelection.None
    private var scanHandler: ScanHandler? = null
    private var lockHandler: LockHandler? = null
    override var hull = template.hull
    private val jumpHandler = JumpHandler(
        jumpDrive = template.jumpDrive
    )
    var mainScreenView = MainScreenView.Front
    val sensorRange: Double
        get() = max(template.shortRangeScopeRange, template.sensorRange * Sensors.boostLevel)
    val throttle
        get() = throttleHandler.requested
    var rudder: Int = 0
    override val systemsDamage
        get() = powerHandler.systemsDamage

    override fun update(time: GameTime, physicsEngine: PhysicsEngine, shipProvider: ShipProvider) {
        powerHandler.update(time)
        updateBeams(time, shipProvider, physicsEngine)
        shieldHandler.update(time, Shields.boostLevel)
        jumpHandler.update(time, Jump.boostLevel)
        updateScan(time, shipProvider)
        updateLock(time, shipProvider)
        throttleHandler.update(time)
        val effectiveThrust = throttleHandler.effectiveThrust(Impulse.boostLevel)
        val effectiveRudder = rudder * template.rudderFactor * Maneuver.boostLevel
        physicsEngine.updateShip(id, effectiveThrust, effectiveRudder)

        physicsEngine.getBodyParameters(id)?.let {
            position = it.position
            rotation = it.rotation
        }

        updateHistory(time)
        updateMapSelection(shipProvider)
    }

    override fun endUpdate(physicsEngine: PhysicsEngine): ShipUpdateResult {
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

    override fun takeDamage(targetSystemType: PoweredSystemType, amount: Double) {
        val hullDamage = shieldHandler.takeDamageAndReportHullDamage(amount)
        if (hullDamage > 0.0) {
            hull -= hullDamage
            powerHandler.takeDamage(targetSystemType, amount)
        }
    }

    override fun targetDestroyed(shipId: ObjectId) {
        if (mapSelection.isShipSelected(shipId)) {
            mapSelection = MapSelection.None
        }
        if (scanHandler?.targetId == shipId) {
            scanHandler = null
        }
        if (lockHandler?.targetId == shipId) {
            lockHandler = null
        }
        scans.remove(shipId)
    }

    fun changeThrottle(value: Int) {
        if (!jumpHandler.jumping) {
            throttleHandler.requested = value
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
            throttleHandler.requested = 0
            rudder = 0
        }
    }

    fun changeRudder(value: Int) {
        if (!jumpHandler.jumping) {
            rudder = value.clamp(-100, 100)
        }
    }

    fun mapClearSelection() {
        mapSelection = MapSelection.None
    }

    fun mapSelectWaypoint(index: Int) {
        if (waypoints.any { it.index == index }) {
            mapSelection = MapSelection.Waypoint(index)
        }
    }

    fun mapSelectShip(targetId: ObjectId) {
        mapSelection = MapSelection.Ship(targetId)
    }

    fun addWaypoint(position: Vector2) {
        (1..waypoints.size * 2 + 1).firstOrNull {
            waypoints.none { waypoint -> waypoint.index == it }
        }?.also {
            waypoints += Waypoint(it, position)
        }
        waypoints.sortBy { it.index }
    }

    fun deleteSelectedWaypoint() {
        mapSelection.also { selection ->
            if (selection is MapSelection.Waypoint) {
                waypoints.removeIf { it.index == selection.index }
                mapSelection = MapSelection.None
            }
        }
    }

    fun startScan() {
        mapSelection.also { selection ->
            if (
                scanHandler == null &&
                selection is MapSelection.Ship &&
                canIncreaseScanLevel(selection.targetId)
            ) {
                scanHandler = ScanHandler(
                    targetId = selection.targetId,
                    boostLevel = { Sensors.boostLevel }
                )
            }
        }
    }

    fun solveScanGame(dimension: Int, value: Double) {
        scanHandler?.apply {
            adjustInput(dimension, value)
        }
    }

    fun abortScan() {
        scanHandler = null
    }

    fun lockTarget(targetId: ObjectId) {
        if (lockHandler?.targetId != targetId) {
            lockHandler = LockHandler(
                targetId = targetId,
                lockingSpeed = template.lockingSpeed,
                boostLevel = { Sensors.boostLevel }
            )
        }
    }

    fun setShieldsUp(value: Boolean) {
        shieldHandler.up = value
    }

    fun toggleShieldsUp() {
        shieldHandler.toggleUp()
    }

    fun startRepair(systemType: PoweredSystemType) {
        powerHandler.startRepair(systemType)
    }

    fun abortRepair() {
        powerHandler.abortRepair()
    }

    fun solveRepairGame(column: Int, row: Int) {
        powerHandler.solveRepairGame(column, row)
    }

    fun setPower(systemType: PoweredSystemType, level: Int) {
        powerHandler.setLevel(systemType, level)
    }

    fun setCoolant(systemType: PoweredSystemType, coolant: Double) {
        powerHandler.setCoolant(systemType, coolant)
    }

    override fun inSensorRange(other: Vector2?) =
        other != null && (other - position).length() <= sensorRange

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id,
            name = designation,
            shipClass = template.className
        )

    fun toMessage() =
        ShipMessage(
            id = id,
            model = template.model,
            designation = designation,
            position = position.twoDigits(),
            rotation = rotation.fiveDigits(),
            beams = beamHandlers.map { it.toMessage(lockHandler) },
            shield = shieldHandler.toMessage(),
            jumpDrive = jumpHandler.toMessage(),
            mainScreenView = mainScreenView,
            frontCamera = template.frontCamera.toMessage()
        )

    fun toNavigationMessage(shipProvider: ShipProvider) =
        NavigationShipMessage(
            id = id,
            position = position.twoDigits(),
            rotation = rotation.fiveDigits(),
            history = history.map { it.second.twoDigits() },
            waypoints = waypoints.map { it.toWaypointMessage() },
            sensorRange = sensorRange.twoDigits(),
            scanProgress = scanHandler?.toMessage(shipProvider)
        )

    fun toShortRangeScopeMessage() =
        ShortRangeScopeMessage(
            shortRangeScopeRange = template.shortRangeScopeRange,
            rotation = rotation.fiveDigits(),
            history = history.map { (it.second - position).twoDigits() },
            waypoints = waypoints.map { it.toWaypointMessage() },
            lockProgress = lockHandler?.toMessage() ?: LockStatus.NoLock,
            beams = beamHandlers.map { it.toMessage(lockHandler) }
        )

    override fun toContactMessage(relativeTo: PlayerShip) =
        ContactMessage(
            id = id,
            model = template.model,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            beams = beamHandlers.map { it.toMessage(lockHandler) },
            shield = shieldHandler.toMessage(),
            jumpAnimation = jumpHandler.toMessage().animation
        )

    override fun toMapContactMessage(relativeTo: PlayerShip) =
        MapContactMessage(
            id = id,
            type = getContactType(relativeTo),
            designation = designation,
            position = position,
            rotation = rotation
        )

    override fun toScopeContactMessage(relativeTo: PlayerShip) =
        ScopeContactMessage(
            id = id,
            type = getContactType(relativeTo),
            designation = designation,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            locked = relativeTo.isLocking(id)
        )

    override fun toShieldMessage() = shieldHandler.toMessage()

    fun toMapSelectionMessage(shipProvider: ShipProvider) =
        mapSelection.let { selection ->
            when (selection) {
                is MapSelection.Waypoint -> {
                    toWaypointMapSelectionMessage(selection)
                }
                is MapSelection.Ship -> {
                    toShipMapSelectionMessage(shipProvider, selection)
                }
                else -> null
            }
        }

    fun toPowerMessage() = powerHandler.toMessage()

    fun toJumpDriveMessage() = jumpHandler.toMessage()

    fun isLocking(targetId: ObjectId) =
        if (lockHandler != null) {
            lockHandler?.targetId == targetId
        } else {
            false
        }

    fun getScanLevel(targetId: ObjectId) = scans[targetId] ?: ScanLevel.None

    private fun updateBeams(
        time: GameTime,
        shipProvider: ShipProvider,
        physicsEngine: PhysicsEngine
    ) {
        beamHandlers.forEach {
            it.update(
                time = time,
                boostLevel = Weapons.boostLevel,
                shipProvider = shipProvider,
                lockHandler = lockHandler,
                physicsEngine = physicsEngine
            )
        }
    }

    private fun updateScan(time: GameTime, shipProvider: ShipProvider) {
        scanHandler?.also {
            val target = shipProvider(it.targetId)
            if (!inSensorRange(target)) {
                scanHandler = null
            }
        }
        scanHandler?.also {
            it.update(time)
            if (it.isComplete) {
                val scan = scans[it.targetId] ?: ScanLevel.None
                scans[it.targetId] = scan.next
                scanHandler = null
            }
        }
    }

    private fun updateLock(time: GameTime, shipProvider: ShipProvider) {
        lockHandler?.also {
            val target = shipProvider(it.targetId)
            if (!inSensorRange(target)) {
                lockHandler = null
            }
        }
        lockHandler?.also {
            if (!it.isComplete) {
                it.update(time)
            }
        }
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

    private fun updateMapSelection(shipProvider: ShipProvider) {
        mapSelection.also {
            if (it is MapSelection.Ship) {
                val selected = shipProvider(it.targetId)
                if (!inSensorRange(selected)) {
                    mapSelection = MapSelection.None
                }
            }
        }
    }

    private fun toWaypointMapSelectionMessage(selection: MapSelection.Waypoint) =
        waypoints.firstOrNull { it.index == selection.index }
            ?.let { waypoint ->
                MapSelectionMessage(
                    position = waypoint.position,
                    label = waypoint.label,
                    bearing = bearingTo(waypoint.position),
                    range = rangeTo(waypoint.position),
                    canDelete = true
                )
            }

    private fun toShipMapSelectionMessage(
        shipProvider: ShipProvider,
        selection: MapSelection.Ship
    ) =
        shipProvider(selection.targetId)?.let { ship ->
            MapSelectionMessage(
                position = ship.position,
                label = ship.designation,
                bearing = bearingTo(ship.position),
                range = rangeTo(ship.position),
                canScan = true
            ).let { message ->
                when (getScanLevel(selection.targetId)) {
                    ScanLevel.Detailed -> {
                        message.copy(
                            hullRatio = (ship.hull / ship.template.hull).fiveDigits(),
                            shield = ship.toShieldMessage(),
                            systemsDamage = ship.systemsDamage
                        )
                    }
                    ScanLevel.Basic -> {
                        message.copy(
                            hullRatio = (ship.hull / ship.template.hull).fiveDigits(),
                            shield = ship.toShieldMessage()
                        )
                    }
                    else -> {
                        message
                    }
                }
            }
        }

    private fun canIncreaseScanLevel(targetId: ObjectId) = getScanLevel(targetId).canBeIncreased

    private fun getContactType(relativeTo: PlayerShip) =
        if (relativeTo.getScanLevel(id) >= ScanLevel.Basic) {
            ContactType.Friendly
        } else {
            ContactType.Unknown
        }

    private val PoweredSystemType.boostLevel
        get() = powerHandler.getBoostLevel(this)

    private fun rangeTo(to: Vector2) =
        (to - position).length().twoDigits()

    private fun bearingTo(to: Vector2) =
        (to - position).angle().toHeading().twoDigits()

    private inner class Waypoint(
        val index: Int,
        val position: Vector2
    ) {

        val label
            get() = "WP$index"

        fun toWaypointMessage() =
            WaypointMessage(
                index = index,
                name = label,
                position = position.twoDigits(),
                relativePosition = (position - this@PlayerShip.position).twoDigits(),
                bearing = (position - this@PlayerShip.position).angle().toHeading().twoDigits()
            )
    }
}

private sealed class MapSelection {

    fun isWaypointSelected(indexToCheck: Int) =
        this is Waypoint && index == indexToCheck

    fun isShipSelected(shipId: ObjectId) =
        this is Ship && targetId == shipId

    object None : MapSelection()

    data class Waypoint(
        val index: Int
    ) : MapSelection()

    data class Ship(
        val targetId: ObjectId
    ) : MapSelection()
}

data class ShipUpdateResult(
    val id: ObjectId,
    val destroyed: Boolean
)

typealias ShipProvider = (ObjectId) -> Ship?
