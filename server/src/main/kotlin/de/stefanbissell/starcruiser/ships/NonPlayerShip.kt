package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ai.ShipAi
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.scenario.Faction
import de.stefanbissell.starcruiser.ships.combat.BeamHandlerContainer
import de.stefanbissell.starcruiser.ships.combat.LockHandler
import de.stefanbissell.starcruiser.ships.combat.ShieldHandler
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.max

class NonPlayerShip(
    override val id: ObjectId = ObjectId.random(),
    override val template: ShipTemplate = carrierTemplate,
    override val faction: Faction,
    override val designation: String = randomShipName(),
    override var position: Vector2 = Vector2(),
    override var rotation: Double = 90.0.toRadians(),
) : Ship {

    private val shipAi = ShipAi(this)
    val powerHandler = SimplePowerHandler(template)
    val beamHandlerContainer = BeamHandlerContainer(template.beams, this)
    val shieldHandler = ShieldHandler(template.shield)
    var scanHandler: TimedScanHandler? = null
    var lockHandler: LockHandler? = null
    val sensorRange: Double
        get() = max(template.shortRangeScopeRange, template.sensorRange)
    var throttle: Int = 0
    var rudder: Int = 0
    override var speed: Vector2 = Vector2()
    override var hull = template.hull
    override val scans = mutableMapOf<ObjectId, ScanLevel>()
    override val systemsDamage
        get() = powerHandler.systemsDamage
    override val updateResult: ShipUpdateResult
        get() = ShipUpdateResult(
            id = id,
            damageEvents = beamHandlerContainer.damageEvents
        )

    override fun update(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ContactList
    ) {
        powerHandler.update(time)
        updateBeams(time, physicsEngine, contactList)
        shieldHandler.update(time)
        updateScan(time, contactList)
        updateLock(time, contactList)
        shipAi.update(time, contactList)
        updatePhysics(physicsEngine)
    }

    override fun takeDamage(targetSystemType: PoweredSystemType, amount: Double, modulation: Int) {
        val hullDamage = shieldHandler.takeDamageAndReportHullDamage(amount, modulation)
        if (hullDamage > 0.0) {
            hull -= hullDamage
            powerHandler.takeDamage(targetSystemType, hullDamage)
        }
    }

    override fun targetDestroyed(id: ObjectId) {
        if (scanHandler?.targetId == id) {
            abortScan()
        }
        if (lockHandler?.targetId == id) {
            abortLock()
        }
        shipAi.targetDestroyed(id)
    }

    override fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id,
            model = template.model,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            beams = toBeamsMessage(),
            shield = toShieldMessage(),
            jumpAnimation = null
        )

    override fun toShieldMessage() = shieldHandler.toMessage()

    override fun toBeamsMessage() = beamHandlerContainer.toMessage(lockHandler)

    override fun inSensorRange(other: Vector2?): Boolean =
        other != null && rangeTo(other) <= sensorRange

    override fun isLocking(targetId: ObjectId) =
        if (lockHandler != null) {
            lockHandler?.targetId == targetId
        } else {
            false
        }

    fun startScan(targetId: ObjectId) {
        scanHandler = TimedScanHandler(targetId, template.scanSpeed)
    }

    fun abortScan() {
        scanHandler = null
    }

    fun startLock(targetId: ObjectId) {
        lockHandler = LockHandler(targetId, template.lockingSpeed)
    }

    fun abortLock() {
        lockHandler = null
    }

    private fun updateBeams(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ContactList
    ) {
        beamHandlerContainer.update(
            time = time,
            contactList = contactList,
            lockHandler = lockHandler,
            physicsEngine = physicsEngine
        )
    }

    private fun updateScan(time: GameTime, contactList: ContactList) {
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

    private fun updateLock(time: GameTime, contactList: ContactList) {
        lockHandler?.apply {
            if (contactList.outOfSensorRange(targetId)) {
                abortLock()
            }
        }
        lockHandler?.update(time)
    }

    private fun updatePhysics(physicsEngine: PhysicsEngine) {
        val effectiveThrust = if (throttle < 0) {
            throttle * template.reverseThrustFactor
        } else {
            throttle * template.aheadThrustFactor
        }
        val effectiveRudder = rudder * template.rudderFactor
        physicsEngine.updateObject(id, effectiveThrust, effectiveRudder)

        physicsEngine.getBodyParameters(id)?.also {
            position = it.position
            rotation = it.rotation
            speed = it.speed
        }
    }
}
