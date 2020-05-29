package de.bissell.starcruiser

import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Mat22
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.World
import java.util.UUID

class PhysicsEngine {

    private val world = World(Vec2())
    private val ships: MutableMap<UUID, Body> = mutableMapOf()

    fun step(time: GameTime) = world.step(time.delta.toFloat(), 6 , 2)

    fun createShip(ship: Ship) {
        val body = world.createBody(BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(ship.position.toVec2())
            allowSleep = false
            linearDamping = 0.5f
            angularDamping = 0.95f
        }).apply {
            createFixture(
                CircleShape().apply {
                    radius = 10f
                },
                0.01f
            )
        }
        ships[ship.id] = body
    }

    fun updateShip(id: UUID, thrust: Double, rudder: Double) {
        ships[id]?.apply {
            applyForceToCenter(
                Mat22.createRotationalTransform(angle).mul(Vec2(thrust.toFloat(), 0f))
            )
            applyTorque(-rudder.toFloat())
        }
    }

    fun getShipStats(id: UUID) =
        ships[id]?.let {
            ShipParameters(
                position = it.position.toVector2(),
                speed = it.linearVelocity.toVector2(),
                rotation = it.angle.toDouble()
            )
        }

    private fun Vec2.toVector2() = Vector2(x.toDouble(), y.toDouble())

    private fun Vector2.toVec2() = Vec2(x.toFloat(), y.toFloat())
}

data class ShipParameters(
    val position: Vector2,
    val speed: Vector2,
    val rotation: Double
)