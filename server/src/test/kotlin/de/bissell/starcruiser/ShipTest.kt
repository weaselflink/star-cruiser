package de.bissell.starcruiser

import de.bissell.starcruiser.ships.Ship
import de.bissell.starcruiser.ships.Waypoint
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty

class ShipTest {

    private val ship = Ship()

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
}
