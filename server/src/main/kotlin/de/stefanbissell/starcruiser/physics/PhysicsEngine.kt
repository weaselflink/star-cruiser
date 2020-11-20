package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipTemplate
import de.stefanbissell.starcruiser.ships.Torpedo
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.callbacks.RayCastCallback
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Mat22
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Fixture
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.contacts.Contact

class PhysicsEngine {

    private val world = World(Vec2()).also {
        it.setContactListener(ContactCallback())
    }
    private val bodies = mutableMapOf<ObjectId, Body>()

    val collisions = mutableListOf<Pair<ObjectId, ObjectId>>()

    fun step(delta: Number) {
        collisions.clear()
        world.step(delta.toFloat(), 6, 2)
    }

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
            fixture {
                shape = PolygonShape().apply {
                    set(points, points.size)
                }
                density = geometry.density.toFloat()
            }
        }
    }

    private fun Torpedo.toBody(sleepingAllowed: Boolean = true) =
        createDynamicBody(
            position,
            rotation,
            speed
        ).apply {
            isSleepingAllowed = sleepingAllowed
            m_userData = this@toBody.id
            createFixture(this@toBody)
        }

    private fun Body.createFixture(torpedo: Torpedo) {
        fixture {
            shape = CircleShape().apply {
                radius = torpedo.template.radius.toFloat()
            }
            density = torpedo.density.toFloat()
            isSensor = true
        }
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
        fixture {
            shape = CircleShape().apply {
                radius = asteroid.radius.toFloat()
            }
            density = 0.02f
        }
    }

    private fun createDynamicBody(
        position: Vector2,
        rotation: Double,
        speed: Vector2? = null
    ) = world.createBody {
        type = BodyType.DYNAMIC
        this.position.set(position.toVec2())
        angle = rotation.toFloat()
        if (speed != null) {
            linearVelocity = speed.toVec2()
        }
        linearDamping = 0.4f
        angularDamping = 0.95f
    }

    private fun Body.fixture(block: FixtureDef.() -> Unit) {
        createFixture(
            FixtureDef().apply(block)
        )
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

    private inner class ContactCallback : ContactListener {
        override fun beginContact(contact: Contact) {
            collisions += contact.objectIds
        }

        override fun endContact(contact: Contact) {}

        override fun preSolve(contact: Contact, oldManifold: Manifold) {}

        override fun postSolve(contact: Contact, impulse: ContactImpulse) {}

        private val Contact.objectIds
            get() = listOf(fixtureA, fixtureB)
                .map { it.body.m_userData as ObjectId }
                .let { Pair(it.first(), it.last()) }
    }
}

data class BodyParameters(
    val position: Vector2,
    val speed: Vector2 = Vector2(),
    val rotation: Double = 0.0,
    val rotationSpeed: Double = 0.0
)
