package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.MapContactMessage
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine

interface ShipInterface {

    val id: ObjectId
    val template: ShipTemplate
    val designation: String
    var position: Vector2
    var rotation: Double
    var hull: Double

    fun update(time: GameTime, physicsEngine: PhysicsEngine, shipProvider: ShipProvider)

    fun endUpdate(physicsEngine: PhysicsEngine): ShipUpdateResult

    fun toContactMessage(relativeTo: Ship): ContactMessage

    fun toMapContactMessage(relativeTo: Ship): MapContactMessage

    fun toScopeContactMessage(relativeTo: Ship): ScopeContactMessage

    fun toShieldMessage(): ShieldMessage

    fun inSensorRange(other: ShipInterface?) = inSensorRange(other?.position)

    fun inSensorRange(other: Asteroid?) = inSensorRange(other?.position)

    fun inSensorRange(other: Vector2?): Boolean

    fun targetDestroyed(shipId: ObjectId)
    fun takeDamage(targetSystemType: PoweredSystemType, amount: Double)
}
