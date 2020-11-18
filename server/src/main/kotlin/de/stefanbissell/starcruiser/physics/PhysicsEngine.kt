package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipTemplate
import de.stefanbissell.starcruiser.ships.Torpedo
import org.jbox2d.callbacks.RayCastCallback
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Mat22
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Fixture
import org.jbox2d.dynamics.World

class PhysicsEngine {

    private val world = World(Vec2())
    private val bodies: MutableMap<ObjectId, Body> = mutableMapOf()

    fun step(delta: Number) = world.step(delta.toFloat(), 6, 2)

    fun addShip(ship: Ship, sleepingAllowed: Boolean = true) {
        bodies[ship.id] = ship.toBody(sleepingAllowed)
    }

    fun addTorpedo(torpedo: Torpedo, sleepingAllowed: Boolean = true) {
        bodies[torpedo.id] = torpedo.toBody(sleepingAllowed)
    }

    fun addAsteroid(asteroid: Asteroid) {
        bodies[asteroid.id] = asteroid.toBody()
    }

    fun removeObject(id: ObjectId) {
        bodies.remove(id)?.also {
            world.destroyBody(it)
        }
    }

    fun updateObject(objectId: ObjectId, thrust: Double, rudder: Double = 0.0) {
        bodies[objectId]?.apply {
            if (thrust != 0.0) {
                applyForceToCenter(
                    Mat22.createRotationalTransform(angle).mul(Vec2(thrust.toFloat(), 0f))
                )
            }
            if (rudder != 0.0) {
                applyTorque(rudder.toFloat())
            }
        }
    }

    fun setShipSpeed(objectId: ObjectId, speed: Vector2) {
        bodies[objectId]?.apply {
            linearVelocity = speed.toVec2()
        }
    }

    fun setShipRotationSpeed(objectId: ObjectId, rotationSpeed: Double) {
        bodies[objectId]?.apply {
            angularVelocity = rotationSpeed.toFloat()
        }
    }

    fun jumpShip(objectId: ObjectId, distance: Int) {
        bodies[objectId]?.apply {
            setTransform(
                position.add(Mat22.createRotationalTransform(angle).mul(Vec2(distance.toFloat(), 0f))),
                angle
            )
        }
    }

    fun getBodyParameters(objectId: ObjectId) =
        bodies[objectId]?.let {
            BodyParameters(
                position = it.position.toVector2(),
                speed = it.linearVelocity.toVector2(),
                rotation = it.angle.toDouble(),
                rotationSpeed = it.angularVelocity.toDouble()
            )
        }

    fun findObstructions(start: Vector2, end: Vector2, ignore: List<ObjectId> = emptyList()): List<ObjectId> {
        val holder = ObstructionHolder()
        world.raycast(holder, start.toVec2(), end.toVec2())
        return holder.obstructions
            .apply { removeAll(ignore) }
            .distinct()
            .toList()
    }

    private fun Ship.toBody(sleepingAllowed: Boolean = true) =
        createDynamicBody(
            position,
            rotation
        ).apply {
            isSleepingAllowed = sleepingAllowed
            m_userData = this@toBody.id
            createFixtures(this@toBody.template)
        }

    private fun Body.createFixtures(shipTemplate: ShipTemplate) {
        shipTemplate.physics.geometry.forEach { geometry ->
            val points = geometry.shape.border.map {
                Vec2(it.x.toFloat(), it.y.toFloat())
            }.toTypedArray()
            createFixture(
                PolygonShape().apply {
                    set(points, points.size)
                },
                geometry.density.toFloat()
            )
        }
    }

    private fun Torpedo.toBody(sleepingAllowed: Boolean = true) =
        createDynamicBody(
            position,
            rotation
        ).apply {
            isSleepingAllowed = sleepingAllowed
            m_userData = this@toBody.id
            createFixtures(this@toBody)
        }

    private fun Body.createFixtures(torpedo: Torpedo) {
        createFixture(
            CircleShape().apply {
                radius = torpedo.radius.toFloat()
            },
            torpedo.density.toFloat()
        )
    }

    private fun Asteroid.toBody() =
        createDynamicBody(
            position,
            rotation
        ).apply {
            m_userData = this@toBody.id
            createFixture(this@toBody)
        }

    private fun Body.createFixture(asteroid: Asteroid) {
        createFixture(
            CircleShape().apply {
                radius = asteroid.radius.toFloat()
            },
            0.02f
        )
    }

    private fun createDynamicBody(
        position: Vector2,
        rotation: Double
    ) = world.createBody {
        type = BodyType.DYNAMIC
        this.position.set(position.toVec2())
        angle = rotation.toFloat()
        linearDamping = 0.4f
        angularDamping = 0.95f
    }

    private fun Vec2.toVector2(): Vector2 =
        Vector2(x.toDouble(), y.toDouble())

    private fun Vector2.toVec2(): Vec2 = Vec2(x.toFloat(), y.toFloat())

    private fun World.createBody(block: BodyDef.() -> Unit): Body = createBody(BodyDef().apply(block))

    private class ObstructionHolder : RayCastCallback {

        val obstructions = mutableListOf<ObjectId>()

        override fun reportFixture(fixture: Fixture?, point: Vec2?, normal: Vec2?, fraction: Float): Float {
            fixture?.body?.let {
                val userData = it.m_userData
                if (userData is ObjectId) {
                    obstructions += userData
                }
            }
            return 1f
        }
    }
}

data class BodyParameters(
    val position: Vector2,
    val speed: Vector2 = Vector2(),
    val rotation: Double = 0.0,
    val rotationSpeed: Double = 0.0
)
