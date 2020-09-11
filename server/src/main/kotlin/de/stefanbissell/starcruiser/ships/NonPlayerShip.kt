package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.MapContactMessage
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ai.ShipAi
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.max

class NonPlayerShip(
    override val id: ObjectId = ObjectId.random(),
    override val template: ShipTemplate = carrierTemplate,
    override val faction: Faction = Faction.Enemy,
    override val designation: String = randomShipName(),
    override var position: Vector2 = Vector2(),
    override var rotation: Double = 90.0.toRadians(),
) : Ship {

    private val shipAi = ShipAi()
    val powerHandler = SimplePowerHandler(template)
    val shieldHandler = ShieldHandler(template.shield)
    val scans = mutableMapOf<ObjectId, ScanLevel>()
    var scanHandler: TimedScanHandler? = null
    var lockHandler: LockHandler? = null
    val sensorRange: Double
        get() = max(template.shortRangeScopeRange, template.sensorRange)
    var throttle: Int = 0
    var rudder: Int = 0
    override var hull = template.hull
    override val systemsDamage
        get() = powerHandler.systemsDamage

    override fun update(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ShipContactList
    ) {
        val shipProvider: ShipProvider = { contactList[it]?.ship }

        powerHandler.update(time)
        shieldHandler.update(time)
        updateScan(time, shipProvider)
        updateLock(time, shipProvider)
        shipAi.update(
            ship = this,
            time = time,
            contactList = contactList
        )

        val effectiveThrust = if (throttle < 0) {
            throttle * template.reverseThrustFactor
        } else {
            throttle * template.aheadThrustFactor
        }
        val effectiveRudder = rudder * template.rudderFactor
        physicsEngine.updateShip(id, effectiveThrust, effectiveRudder)

        physicsEngine.getBodyParameters(id)?.let {
            position = it.position
            rotation = it.rotation
        }
    }

    override fun endUpdate(physicsEngine: PhysicsEngine): ShipUpdateResult {
        shieldHandler.endUpdate()
        val destroyed = hull <= 0.0
        return ShipUpdateResult(
            id = id,
            destroyed = destroyed
        )
    }

    override fun takeDamage(targetSystemType: PoweredSystemType, amount: Double) {
        val hullDamage = shieldHandler.takeDamageAndReportHullDamage(amount)
        if (hullDamage > 0.0) {
            hull -= hullDamage
            powerHandler.takeDamage(targetSystemType, hullDamage)
        }
    }

    override fun targetDestroyed(shipId: ObjectId) {
        if (scanHandler?.targetId == shipId) {
            scanHandler = null
        }
        if (lockHandler?.targetId == shipId) {
            lockHandler = null
        }
    }

    override fun toContactMessage(relativeTo: PlayerShip) =
        ContactMessage(
            id = id,
            model = template.model,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            beams = emptyList(),
            shield = shieldHandler.toMessage(),
            jumpAnimation = null
        )

    override fun toMapContactMessage(relativeTo: PlayerShip) =
        MapContactMessage(
            id = id,
            type = relativeTo.getContactType(this),
            designation = designation,
            position = position,
            rotation = rotation
        )

    override fun toScopeContactMessage(relativeTo: PlayerShip) =
        ScopeContactMessage(
            id = id,
            type = relativeTo.getContactType(this),
            designation = designation,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            locked = relativeTo.isLocking(id)
        )

    override fun toShieldMessage() = shieldHandler.toMessage()

    override fun inSensorRange(other: Vector2?): Boolean =
        other != null && rangeTo(other) <= sensorRange

    override fun getScanLevel(targetId: ObjectId) =
        scans[targetId] ?: ScanLevel.None

    override fun getContactType(other: Ship) =
        if (getScanLevel(other.id) >= ScanLevel.Basic) {
            if (other.faction == faction) {
                ContactType.Friendly
            } else {
                ContactType.Enemy
            }
        } else {
            ContactType.Unknown
        }

    fun startScan(targetId: ObjectId) {
        scanHandler = TimedScanHandler(targetId, template.scanSpeed)
    }

    fun startLock(targetId: ObjectId) {
        lockHandler = LockHandler(targetId, template.lockingSpeed)
    }

    private fun updateScan(time: GameTime, shipProvider: ShipProvider) {
        scanHandler?.apply {
            val target = shipProvider(targetId)
            if (!inSensorRange(target)) {
                scanHandler = null
            }
        }
        scanHandler?.apply {
            update(time)
            if (isComplete) {
                val scan = scans[targetId] ?: ScanLevel.None
                scans[targetId] = scan.next
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
}
