package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.emptyContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.toRadians
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.math.PI

class HelmAiTest {

    private val ship = NonPlayerShip(rotation = 0.0)
    private val helmAi = HelmAi()

    @Test
    fun `sets rudder to port`() {
        helmAi.targetRotation = PI * 0.5

        helmAi.execute(ship, GameTime(), emptyContactList(ship))

        expectThat(ship.rudder).isEqualTo(100)
    }

    @Test
    fun `sets rudder to starboard`() {
        helmAi.targetRotation = -PI * 0.5

        helmAi.execute(ship, GameTime(), emptyContactList(ship))

        expectThat(ship.rudder).isEqualTo(-100)
    }

    @Test
    fun `centers rudder`() {
        helmAi.targetRotation = 0.1.toRadians()
        ship.rudder = 100

        helmAi.execute(ship, GameTime(), emptyContactList(ship))

        expectThat(ship.rudder).isEqualTo(0)
    }
}
