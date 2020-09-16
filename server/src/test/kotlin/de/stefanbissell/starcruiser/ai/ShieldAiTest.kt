package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.PlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class ShieldAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime.atEpoch()
    private val shieldAi = ShieldAi()

    private var contactList = emptyList<Ship>()

    @Test
    fun `lowers shields when no threats known`() {
        expectThat(ship.shieldHandler.up)
            .isTrue()

        executeAi()

        expectThat(ship.shieldHandler.up)
            .isFalse()
    }

    @Test
    fun `lowers shields when no threats nearer than 500`() {
        addHostileShip(p(501, 0))
        expectThat(ship.shieldHandler.up)
            .isTrue()

        executeAi()

        expectThat(ship.shieldHandler.up)
            .isFalse()
    }

    @Test
    fun `raises shields when threats within 500`() {
        addHostileShip(p(499, 0))
        ship.shieldHandler.toggleUp()
        expectThat(ship.shieldHandler.up)
            .isFalse()

        executeAi()

        expectThat(ship.shieldHandler.up)
            .isTrue()
    }

    @Test
    fun `keeps shields raised when threats within 500`() {
        addHostileShip(p(499, 0))
        expectThat(ship.shieldHandler.up)
            .isTrue()

        executeAi()

        expectThat(ship.shieldHandler.up)
            .isTrue()
    }

    private fun addHostileShip(position: Vector2) {
        val target = PlayerShip(position = position)
        contactList = listOf(target)
        ship.scans[target.id] = ScanLevel.Detailed
    }

    private fun executeAi() {
        shieldAi.execute(ship, time, ShipContactList(ship, contactList))
    }
}
