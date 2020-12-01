package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactShieldMessage
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.scenario.Faction
import de.stefanbissell.starcruiser.ships.DynamicObject
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.PI

class Torpedo(
    val launcherId: ObjectId,
    override val id: ObjectId = ObjectId.random(),
    override val faction: Faction,
    override val designation: String = randomShipName(),
    override var position: Vector2 = Vector2(),
    override var rotation: Double = 90.0.toRadians(),
    override var speed: Vector2 = Vector2(),
    val template: TorpedoTemplate = TorpedoTemplate()
) : DynamicObject {

    override val shipType: ShipType
        get() = ShipType.Projectile
    var timeSinceLaunch = 0.0
    var destroyed = false
    val density
        get() = template.mass / (0.5 * PI * template.radius * template.radius)

    fun update(
        time: GameTime,
        physicsEngine: PhysicsEngine
    ) {
        timeSinceLaunch += time.delta
        if (timeSinceLaunch > template.maxBurnTime) {
            destroyed = true
        }

        physicsEngine.updateObject(id, template.thrust)

        physicsEngine.getBodyParameters(id)?.also {
            position = it.position
            rotation = it.rotation
            speed = it.speed
        }
    }

    override fun applyDamage(damageEvent: DamageEvent) {
        destroyed = true
    }

    override fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id,
            model = template.model,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            beams = emptyList(),
            shield = ContactShieldMessage(),
            jumpAnimation = null
        )
}
