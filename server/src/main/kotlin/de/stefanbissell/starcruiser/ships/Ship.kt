package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.BeamMessage
import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine

interface Ship {

    val id: ObjectId
    val template: ShipTemplate
    val faction: Faction
    val designation: String
    var position: Vector2
    var rotation: Double
    val speed: Vector2
    var hull: Double
    val systemsDamage: Map<PoweredSystemType, Double>

    fun update(
        time: GameTime,
        physicsEngine: PhysicsEngine,
        contactList: ShipContactList = ShipContactList(this, emptyMap())
    )

    fun endUpdate(physicsEngine: PhysicsEngine): ShipUpdateResult

    fun toContactMessage(relativeTo: Ship): ContactMessage

    fun toShieldMessage(): ShieldMessage

    fun toBeamMessages(): List<BeamMessage>

    fun isLocking(targetId: ObjectId): Boolean

    fun inSensorRange(other: Ship?) = inSensorRange(other?.position)

    fun inSensorRange(other: Asteroid?) = inSensorRange(other?.position)

    fun inSensorRange(other: Vector2?): Boolean

    fun targetDestroyed(shipId: ObjectId)

    fun takeDamage(targetSystemType: PoweredSystemType, amount: Double)

    fun getScanLevel(targetId: ObjectId): ScanLevel

    fun getContactType(other: Ship) =
        if (getScanLevel(other.id) >= ScanLevel.Basic) {
            if (other.faction == faction) {
                ContactType.Friendly
            } else {
                ContactType.Enemy
            }
        } else {
            ContactType.Unknown
        }

    fun rangeTo(other: Vector2) =
        (other - position).length()
}
