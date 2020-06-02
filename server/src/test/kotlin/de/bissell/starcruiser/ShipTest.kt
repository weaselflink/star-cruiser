package de.bissell.starcruiser

import de.bissell.starcruiser.ships.Ship
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.time.Instant

class ShipTest {

    private val ship = Ship(
        position = Vector2(3, -4)
    )
    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val physicsEngine = mockk<PhysicsEngine>(relaxed = true)

    @Test
    fun `updates positive thrust`() {
        ship.changeThrottle(100)
        time.update(Instant.EPOCH.plusSeconds(2))
        ship.update(time, physicsEngine)

        expectThat(ship.toMessage().thrust).isNear(ship.template.throttleResponsiveness * 2)
    }

    @Test
    fun `updates negative thrust`() {
        ship.changeThrottle(-100)
        time.update(Instant.EPOCH.plusSeconds(2))
        ship.update(time, physicsEngine)

        expectThat(ship.toMessage().thrust).isNear(ship.template.throttleResponsiveness * -2)
    }

    @Test
    fun `sets positive rudder`() {
        ship.changeRudder(50)

        expectThat(ship.toMessage().rudder).isEqualTo(50)
    }

    @Test
    fun `sets negative rudder`() {
        ship.changeRudder(-50)

        expectThat(ship.toMessage().rudder).isEqualTo(-50)
    }

    @Test
    fun `can add waypoint`() {
        ship.addWaypoint(Vector2(5, -4))

        expectThat(ship.toMessage().waypoints).containsExactly(
            WaypointMessage(1, "WP1", Vector2(5, -4), Vector2(2, 0))
        )
    }

    @Test
    fun `can delete waypoint`() {
        ship.addWaypoint(Vector2(5, -4))
        ship.deleteWaypoint(1)

        expectThat(ship.toMessage().waypoints).isEmpty()
    }

    @Test
    fun `keeps index after deletion`() {
        ship.addWaypoint(Vector2(5, -4))
        ship.addWaypoint(Vector2(10, -4))
        ship.deleteWaypoint(1)

        expectThat(ship.toMessage().waypoints).containsExactly(
            WaypointMessage(2, "WP2", Vector2(10, -4), Vector2(7, 0))
        )
    }

    @Test
    fun `fills waypoint slot`() {
        ship.addWaypoint(Vector2(5, -4))
        ship.addWaypoint(Vector2(10, -4))
        ship.deleteWaypoint(1)
        ship.addWaypoint(Vector2(15, -4))

        expectThat(ship.toMessage().waypoints).containsExactly(
            WaypointMessage(2, "WP2", Vector2(10, -4), Vector2(7, 0)),
            WaypointMessage(1, "WP1", Vector2(15, -4), Vector2(12, 0))
        )
    }

    @Test
    fun `does not start scan with scan in progress`() {
        val target1 = Ship()
        val target2 = Ship()

        ship.startScan(target1.id)
        ship.startScan(target2.id)

        expectThat(ship.toMessage().scanProgress).isNotNull()
            .get { targetId }.isEqualTo(target1.id.serialize())
    }

    @Test
    fun `scans target`() {
        val target = Ship()

        ship.startScan(target.id)
        time.update(Instant.EPOCH.plusSeconds(4))
        ship.update(time, physicsEngine)

        expectThat(target.toContactMessage(ship).type).isEqualTo(ContactType.Unknown)

        time.update(Instant.EPOCH.plusSeconds(6))
        ship.update(time, physicsEngine)

        expectThat(target.toContactMessage(ship).type).isEqualTo(ContactType.Friendly)
    }

    @Test
    fun `does not scan target with maximum scan level`() {
        val target = Ship()

        ship.startScan(target.id)
        time.update(Instant.EPOCH.plusSeconds(6))
        ship.update(time, physicsEngine)

        expectThat(target.toContactMessage(ship).type).isEqualTo(ContactType.Friendly)

        ship.startScan(target.id)

        expectThat(ship.toMessage().scanProgress).isNull()
    }

    @Test
    fun `updates physics engine`() {
        ship.changeThrottle(50)
        ship.changeRudder(50)
        time.update(Instant.EPOCH.plusSeconds(2))
        ship.update(time, physicsEngine)

        verify(exactly = 1) {
            physicsEngine.updateShip(
                ship.id,
                50 * ship.template.aheadThrustFactor,
                50 * ship.template.rudderFactor
            )
        }
    }

    @Test
    fun `takes values from physics engine`() {
        every {
            physicsEngine.getShipStats(ship.id)
        } returns ShipParameters(
            position = Vector2(1, 2),
            speed = Vector2(3, 4),
            rotation = 5.0
        )

        ship.update(time, physicsEngine)

        ship.toMessage().also {
            expectThat(it.position).isEqualTo(Vector2(1, 2))
            expectThat(it.speed).isEqualTo(Vector2(3, 4))
            expectThat(it.rotation).isEqualTo(5.0)
        }

    }
}
