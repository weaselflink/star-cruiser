package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ships.Ship

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
            model = "asteroid01",
            radius = radius,
            position = position,
            relativePosition = position - relativeTo.position,
            rotation = rotation
        )
}
