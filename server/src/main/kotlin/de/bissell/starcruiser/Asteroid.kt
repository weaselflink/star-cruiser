package de.bissell.starcruiser

import de.bissell.starcruiser.ships.Ship
import java.util.*

class Asteroid(
    val id: ObjectId = ObjectId.random(),
    var position: Vector2 = Vector2(),
    var rotation: Double = 0.0,
    val radius: Double = 10.0
) {

    fun update(physicsEngine: PhysicsEngine) {
        physicsEngine.getBodyParameters(id)?.let {
            position = it.position
            rotation = it.rotation
        }
    }

    fun toMessage(relativeTo: Ship) =
        AsteroidMessage(
            id = id,
            radius = radius,
            position = position,
            relativePosition = position - relativeTo.position,
            rotation = rotation
        )

    fun toScopeMessage(relativeTo: Ship) =
        ScopeAsteroidMessage(
            id = id,
            radius = radius,
            relativePosition = position - relativeTo.position,
            rotation = rotation
        )
}
