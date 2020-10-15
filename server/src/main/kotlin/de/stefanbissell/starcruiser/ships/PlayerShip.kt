package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.MainScreenView
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
import de.stefanbissell.starcruiser.ShipMessage
import de.stefanbissell.starcruiser.ShortRangeScopeMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.scenario.Faction
import de.stefanbissell.starcruiser.toIntHeading
import de.stefanbissell.starcruiser.toRadians
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class PlayerShip(
    override val id: ObjectId = ObjectId.random(),
    override val template: ShipTemplate = cruiserTemplate,
    override val faction: Faction,
    override val designation: String = randomShipName(),
    override var position: Vector2 = Vector2(),
    override var rotation: Double = 90.0.toRadians()
) : Ship {

    private val waypoints: MutableList<Waypoint> = mutableListOf()
    private val history = mutableListOf<Pair<Double, Vector2>>()
    private val throttleHandler = ThrottleHandler(template)
    private val powerHandler = PowerHandler(template)
    private val beamHandlers = template.beams.map { BeamHandler(it, this) }
    private val shieldHandler = ShieldHandler(template.shield)
    private var mapSelection: MapSelection = MapSelection.None
    private var scanHandler: ScanHandler? = null
    private var lockHandler: LockHandler? = null
    override var speed: Vector2 = Vector2()
    override var hull = template.hull
    override val scans = mutableMapOf<ObjectId, ScanLevel>()
    override val systemsDamage
        get() = powerHandler.systemsDamage
    private val jumpHandler = JumpHandler(
        jumpDrive = template.jumpDrive
    )
    var mainScreenView = MainScreenView.Front
    val sensorRange: Double
        get() = max(template.shortRangeScopeRange, template.sensorRange * Sensors.boostLevel)
    var throttle: Int
        get() = throttleHandler.requested
        set(value) {
            if (!jumpHandler.jumping) {
                throttleHandler.requested = value
            }
        }
    var rudder: Int = 0
        set(value) {
            if (!jumpHandler.jumping) {
                field = value.clamp(-100, 100)
            }
        }

    override fun update(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ShipContactList
    ) {
        powerHandler.update(time)
        updateBeams(time, physicsEngine, contactList)
        shieldHandler.update(time, Shields.boostLevel)
        jumpHandler.update(time, Jump.boostLevel)
        updateScan(time, contactList)
        updateLock(time, contactList)
        throttleHandler.update(time)
        updatePhysics(physicsEngine)
        updateHistory(time)
        updateMapSelection(contactList)
    }

    override fun endUpdate(
        time: GameTime,
        physicsEngine: PhysicsEngine
    ): ShipUpdateResult {
        shieldHandler.endUpdate(time)
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
            abortScan()
        }
        if (lockHandler?.targetId == shipId) {
            lockHandler = null
        }
        scans.remove(shipId)
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
            frontCamera = template.frontCamera.toMessage(),
            leftCamera = template.leftCamera.toMessage(),
            rightCamera = template.rightCamera.toMessage(),
            rearCamera = template.rearCamera.toMessage()
        )

    fun toNavigationMessage(contactList: ShipContactList) =
        NavigationShipMessage(
            id = id,
            position = position.twoDigits(),
            rotation = rotation.fiveDigits(),
            history = history.map { it.second.twoDigits() },
            waypoints = waypoints.map { it.toWaypointMessage() },
            sensorRange = sensorRange.twoDigits(),
            scanProgress = scanHandler?.toMessage(contactList)
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

    override fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id,
            model = template.model,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            beams = toBeamMessages(),
            shield = toShieldMessage(),
            jumpAnimation = jumpHandler.toMessage().animation
        )

    override fun toShieldMessage() = shieldHandler.toMessage()

    override fun toBeamMessages() = beamHandlers.map { it.toMessage(lockHandler) }

    fun toMapSelectionMessage(contactList: ShipContactList) =
        mapSelection.let { selection ->
            when (selection) {
                is MapSelection.Waypoint -> {
                    toWaypointMapSelectionMessage(selection)
                }
                is MapSelection.Ship -> {
                    toShipMapSelectionMessage(contactList, selection)
                }
                else -> null
            }
        }

    fun toPowerMessage() = powerHandler.toMessage()

    fun toJumpDriveMessage() = jumpHandler.toMessage()

    override fun isLocking(targetId: ObjectId) =
        if (lockHandler != null) {
            lockHandler?.targetId == targetId
        } else {
            false
        }

    private fun updateBeams(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ShipContactList
    ) {
        beamHandlers.forEach {
            it.update(
                time = time,
                boostLevel = Weapons.boostLevel,
                contactList = contactList,
                lockHandler = lockHandler,
                physicsEngine = physicsEngine
            )
        }
    }

    private fun updateScan(time: GameTime, contactList: ShipContactList) {
        scanHandler?.apply {
            if (contactList.outOfSensorRange(targetId)) {
                abortScan()
            }
        }
        scanHandler?.apply {
            update(time)
            val target = contactList[targetId]
            if (isComplete) {
                val scan = target?.scanLevel ?: ScanLevel.None
                scans[targetId] = scan.next
                abortScan()
            }
        }
    }

    private fun updateLock(time: GameTime, contactList: ShipContactList) {
        lockHandler?.apply {
            if (contactList.outOfSensorRange(targetId)) {
                lockHandler = null
            }
        }
        lockHandler?.update(time)
    }

    private fun updatePhysics(physicsEngine: PhysicsEngine) {
        val effectiveThrust = throttleHandler.effectiveThrust(Impulse.boostLevel)
        val effectiveRudder = rudder * template.rudderFactor * Maneuver.boostLevel
        physicsEngine.updateShip(id, effectiveThrust, effectiveRudder)

        physicsEngine.getBodyParameters(id)?.let {
            position = it.position
            rotation = it.rotation
            speed = it.speed
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

    private fun updateMapSelection(contactList: ShipContactList) {
        mapSelection.also {
            if (it is MapSelection.Ship && contactList.outOfSensorRange(it.targetId)) {
                mapSelection = MapSelection.None
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
                    range = rangeTo(waypoint.position).roundToInt(),
                    canDelete = true
                )
            }

    private fun toShipMapSelectionMessage(
        contactList: ShipContactList,
        selection: MapSelection.Ship
    ) =
        contactList[selection.targetId]?.let { contact ->
            MapSelectionMessage(
                position = contact.position,
                label = contact.designation,
                bearing = bearingTo(contact.position),
                range = rangeTo(contact.position).roundToInt(),
                canScan = true
            ).let { message ->
                val ship = contact.ship
                when (contact.scanLevel) {
                    ScanLevel.Detailed -> {
                        message.copy(
                            hullRatio = (max(0.0, ship.hull) / ship.template.hull).fiveDigits(),
                            shield = ship.toShieldMessage(),
                            systemsDamage = ship.systemsDamage,
                            canScan = false
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

    private val PoweredSystemType.boostLevel
        get() = powerHandler.getBoostLevel(this)

    private fun bearingTo(to: Vector2) =
        (to - position).angle().toIntHeading()

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
                relativePosition = (position - this@PlayerShip.position).twoDigits()
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
