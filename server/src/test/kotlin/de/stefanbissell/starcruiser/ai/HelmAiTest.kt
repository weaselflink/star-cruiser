package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.emptyContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.toRadians
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
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
    fun `ends turn when target rotation reached`() {
        helmAi.targetRotation = 0.1.toRadians()
        ship.rudder = 100

        helmAi.execute(ship, GameTime(), emptyContactList(ship))

        expectThat(helmAi.targetRotation).isNull()
        expectThat(ship.rudder).isEqualTo(0)
    }

    @Test
    fun `minimal rudder after calculated point`() {
        helmAi.targetRotation = PI * 0.5

        helmAi.execute(ship, GameTime(), emptyContactList(ship))

        expectThat(ship.rudder).isEqualTo(100)

        ship.rotation = PI * 0.5 - 0.3.toRadians()

        helmAi.execute(ship, GameTime(), emptyContactList(ship))

        expectThat(helmAi.targetRotation).isNotNull()
        expectThat(ship.rudder).isEqualTo(10)
    }
}
