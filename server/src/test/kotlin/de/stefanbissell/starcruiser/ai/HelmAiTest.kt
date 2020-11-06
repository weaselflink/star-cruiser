package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TestFactions
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

    private val time = GameTime.atEpoch()
    private val ship = NonPlayerShip(
        faction = TestFactions.neutral,
        rotation = 0.0
    )
    private val helmAi = HelmAi()

    @Test
    fun `sets rudder to port`() {
        helmAi.targetRotation = PI * 0.5

        executeAi()

        expectThat(ship.rudder).isEqualTo(100)
    }

    @Test
    fun `sets rudder to starboard`() {
        helmAi.targetRotation = -PI * 0.5

        executeAi()

        expectThat(ship.rudder).isEqualTo(-100)
    }

    @Test
    fun `ends turn when target rotation reached`() {
        helmAi.targetRotation = PI * 0.5
        ship.rudder = 100

        executeAi()

        ship.rotation = PI * 0.5 - 0.1.toRadians()

        executeAi()

        expectThat(helmAi.targetRotation).isNull()
        expectThat(ship.rudder).isEqualTo(0)
    }

    @Test
    fun `ends turn when overshooting target rotation`() {
        helmAi.targetRotation = PI * 0.5
        ship.rudder = 100

        executeAi()

        ship.rotation = PI * 0.5 + 10.0.toRadians()

        executeAi()

        expectThat(helmAi.targetRotation).isNull()
        expectThat(ship.rudder).isEqualTo(0)
    }

    @Test
    fun `minimal rudder after calculated point`() {
        helmAi.targetRotation = PI * 0.5

        executeAi()

        expectThat(ship.rudder).isEqualTo(100)

        ship.rotation = PI * 0.5 - 0.3.toRadians()

        executeAi()

        expectThat(helmAi.targetRotation).isNotNull()
        expectThat(ship.rudder).isEqualTo(10)
    }

    @Test
    fun `starts second turn`() {
        helmAi.targetRotation = PI * 0.5

        executeAi()

        ship.rotation = PI * 0.5

        executeAi()

        expectThat(helmAi.targetRotation).isNull()

        helmAi.targetRotation = PI

        executeAi()

        expectThat(ship.rudder).isEqualTo(100)

        ship.rotation = PI

        executeAi()

        expectThat(helmAi.targetRotation).isNull()
        expectThat(ship.rudder).isEqualTo(0)
    }

    private fun executeAi() {
        helmAi.execute(AiState(ship, time, emptyContactList(ship)))
    }
}
