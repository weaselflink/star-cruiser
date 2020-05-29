package de.bissell.starcruiser

import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Mat22
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.World
import java.util.*

class PhysicsEngine {

    private val world = World(Vec2())
    private val ships: MutableMap<UUID, Body> = mutableMapOf()

    fun step(time: GameTime) = world.step(time.delta.toFloat(), 6, 2)

    fun addShip(ship: Ship) {
        ships[ship.id] = ship.toBody()
    }

    fun updateShip(id: UUID, thrust: Double, rudder: Double) {
        ships[id]?.apply {
            applyForceToCenter(
                Mat22.createRotationalTransform(angle).mul(Vec2(thrust.toFloat(), 0f))
            )
            applyTorque(rudder.toFloat())
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

    private fun Ship.toBody() =
        world.createBody {
            type = BodyType.DYNAMIC
            position.set(this@toBody.position.toVec2())
            allowSleep = false
            awake = true
            linearDamping = 0.4f
            angularDamping = 0.95f
        }.apply {
            createFixture(this@toBody)
        }

    private fun Body.createFixture(ship: Ship) =
        createFixture(
            PolygonShape().apply {
                setAsBox(12f, 5f)
            },
            ship.template.density.toFloat()
        )

    private fun Vec2.toVector2(): Vector2 = Vector2(x.toDouble(), y.toDouble())

    private fun Vector2.toVec2(): Vec2 = Vec2(x.toFloat(), y.toFloat())

    private fun World.createBody(block: BodyDef.() -> Unit): Body = createBody(BodyDef().apply(block))
}

data class ShipParameters(
    val position: Vector2,
    val speed: Vector2,
    val rotation: Double
)
