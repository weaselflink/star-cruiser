package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ShipType
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.scenario.Faction
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

    fun endUpdate(): ShipUpdateResult {
        return ShipUpdateResult(id, destroyed)
    }

    override fun takeDamage(targetSystemType: PoweredSystemType, amount: Double, modulation: Int) {
        destroyed = true
    }
}
