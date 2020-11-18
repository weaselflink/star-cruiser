package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.scenario.Faction
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.PI

class Torpedo(
    val id: ObjectId = ObjectId.random(),
    val faction: Faction,
    val designation: String = randomShipName(),
    var position: Vector2 = Vector2(),
    var rotation: Double = 90.0.toRadians(),
    var speed: Vector2 = Vector2(),
    val radius: Double = 1.0,
    private val mass: Double = 100.0,
    private val thrust: Double = 50.0
) {

    val density
        get() = mass / (0.5 * PI * radius * radius)

    fun update(
        physicsEngine: PhysicsEngine
    ) {
        physicsEngine.updateObject(id, thrust)

        physicsEngine.getBodyParameters(id)?.also {
            position = it.position
            rotation = it.rotation
            speed = it.speed
        }
    }

    fun endUpdate(): ShipUpdateResult {
        return ShipUpdateResult(id, false)
    }
}
