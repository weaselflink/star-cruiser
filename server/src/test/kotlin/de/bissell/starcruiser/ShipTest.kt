package de.bissell.starcruiser

import de.bissell.starcruiser.ships.Ship
import de.bissell.starcruiser.ships.Waypoint
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.time.Instant

class ShipTest {

    private val ship = Ship()
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
        ship.addWaypoint(Vector2(5.0, -4.0))

        expectThat(ship.waypoints).containsExactly(
            Waypoint(1, Vector2(5.0, -4.0))
        )
    }

    @Test
    fun `can delete waypoint`() {
        ship.addWaypoint(Vector2(5.0, -4.0))
        ship.deleteWaypoint(1)

        expectThat(ship.waypoints).isEmpty()
    }

    @Test
    fun `keeps index after deletion`() {
        ship.addWaypoint(Vector2(5.0, -4.0))
        ship.addWaypoint(Vector2(10.0, -4.0))
        ship.deleteWaypoint(1)

        expectThat(ship.waypoints).containsExactly(
            Waypoint(2, Vector2(10.0, -4.0))
        )
    }

    @Test
    fun `fills waypoint slot`() {
        ship.addWaypoint(Vector2(5.0, -4.0))
        ship.addWaypoint(Vector2(10.0, -4.0))
        ship.deleteWaypoint(1)
        ship.addWaypoint(Vector2(15.0, -4.0))

        expectThat(ship.waypoints).containsExactlyInAnyOrder(
            Waypoint(1, Vector2(15.0, -4.0)),
            Waypoint(2, Vector2(10.0, -4.0))
        )
    }

    @Test
    fun `does not start scan with scan in progress`() {
        val target1 = Ship()
        val target2 = Ship()

        ship.startScan(target1.id)
        ship.startScan(target2.id)

        expectThat(ship.toMessage().scanProgress).isNotNull()
            .get { targetId }.isEqualTo(target1.id.toString())
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
}
