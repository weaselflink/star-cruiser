package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.BeamsMessage
import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.SimpleShieldMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import kotlin.math.max

interface Ship : DynamicObject {

    val template: ShipTemplate
    override var position: Vector2
    override var rotation: Double
    var hull: Double
    val destroyed
        get() = hull <= 0.0
    val systemsDamage: Map<PoweredSystemType, Double>
    val scans: MutableMap<ObjectId, ScanLevel>
    override val shipType: ShipType
        get() = ShipType.Vessel
    val updateResult: ShipUpdateResult

    fun update(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ContactList = ContactList(this)
    )

    fun toContactMessage(relativeTo: Ship): ContactMessage

    fun toShieldMessage(): ShieldMessage

    fun toSimpleShieldMessage() =
        toShieldMessage().let {
            SimpleShieldMessage(
                up = it.up,
                ratio = (max(0.0, it.strength) / it.max).fiveDigits()
            )
        }

    fun toBeamsMessage(): BeamsMessage

    fun isLocking(targetId: ObjectId): Boolean

    fun inSensorRange(other: DynamicObject?) = inSensorRange(other?.position)

    fun inSensorRange(other: Asteroid?) = inSensorRange(other?.position)

    fun inSensorRange(other: Vector2?): Boolean

    fun targetDestroyed(shipId: ObjectId)

    fun getScanLevel(targetId: ObjectId) =
        scans[targetId] ?: ScanLevel.None

    fun getContactType(other: DynamicObject) =
        getScanLevel(other.id).let { scanLevel ->
            if (other.faction == faction) {
                ContactType.Friendly
            } else if (scanLevel >= ScanLevel.Basic) {
                if (faction isHostileTo other.faction) {
                    ContactType.Enemy
                } else {
                    ContactType.Neutral
                }
            } else {
                ContactType.Unknown
            }
        }

    fun rangeTo(other: Vector2) =
        (other - position).length()
}
