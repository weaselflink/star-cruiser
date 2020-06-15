package de.bissell.starcruiser

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

    companion object {
        private fun ObjectId.Companion.random() = ObjectId(UUID.randomUUID().toString())
    }
}
