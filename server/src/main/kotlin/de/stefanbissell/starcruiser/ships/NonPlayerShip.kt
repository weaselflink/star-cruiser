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
        contactList: List<Ship>,
        shipProvider: ShipProvider
    ) {
        powerHandler.update(time)
        shieldHandler.update(time)
        updateScan(time, shipProvider)
        shipAi.update(
            ship = this,
            time = time,
            contactList = contactList,
            shipProvider = shipProvider
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

    override fun targetDestroyed(shipId: ObjectId) {}

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

    override fun inSensorRange(other: Vector2?): Boolean =
        other != null && (other - position).length() <= sensorRange

    override fun getScanLevel(targetId: ObjectId) =
        scans[targetId] ?: ScanLevel.None

    override fun getContactType(relativeTo: Ship) =
        if (relativeTo.getScanLevel(id) >= ScanLevel.Basic) {
            if (relativeTo.faction == faction) {
                ContactType.Friendly
            } else {
                ContactType.Enemy
            }
        } else {
            ContactType.Unknown
        }

    private fun updateScan(time: GameTime, shipProvider: ShipProvider) {
        scanHandler?.also {
            val target = shipProvider(it.targetId)
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
}
