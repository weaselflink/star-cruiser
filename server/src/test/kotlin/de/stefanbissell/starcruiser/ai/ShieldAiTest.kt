package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.time.Instant

class ShieldAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime(Instant.EPOCH)
    private val shieldAi = ShieldAi()

    @Test
    fun `raises shields when possible`() {
        ship.shieldHandler.toggleUp()
        expectThat(ship.shieldHandler.up)
            .isFalse()

        executeAi()

        expectThat(ship.shieldHandler.up)
            .isTrue()
    }

    private fun executeAi() {
        shieldAi.execute(ship, time, emptyList()) { null }
    }
}
