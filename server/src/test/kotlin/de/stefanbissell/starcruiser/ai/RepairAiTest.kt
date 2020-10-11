package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class RepairAiTest {

    private val ship = NonPlayerShip(faction = TestFactions.neutral)
    private val time = GameTime.atEpoch()
    private val repairAi = RepairAi()

    @Test
    fun `starts repairing damaged system`() {
        ship.shieldHandler.toggleUp()
        ship.takeDamage(PoweredSystemType.Jump, 1.0)
        expectThat(ship.powerHandler.repairing)
            .isFalse()

        executeAi()

        expectThat(ship.powerHandler.repairing)
            .isTrue()
    }

    private fun executeAi() {
        repairAi.execute(ship, time, ShipContactList(ship, emptyMap()))
    }
}
