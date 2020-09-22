package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.physics.PhysicsEngine
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
            radius = radius.twoDigits(),
            position = position.twoDigits(),
            relativePosition = relativePosition(relativeTo),
            rotation = rotation.fiveDigits()
        )

    fun toScopeMessage(relativeTo: Ship) =
        ScopeAsteroidMessage(
            id = id,
            radius = radius.twoDigits(),
            relativePosition = relativePosition(relativeTo),
            rotation = rotation.fiveDigits()
        )

    fun toMapMessage() =
        MapAsteroidMessage(
            id = id,
            radius = radius.twoDigits(),
            position = position.twoDigits(),
            rotation = rotation.fiveDigits()
        )

    private fun relativePosition(relativeTo: Ship) =
        (position - relativeTo.position).twoDigits()
}
