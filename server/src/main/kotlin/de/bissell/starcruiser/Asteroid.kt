package de.bissell.starcruiser

class Asteroid(
    val id: ObjectId,
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
}
