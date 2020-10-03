package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.math.PI

class PatrolAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime.atEpoch()
    private val helmAi = HelmAi()
    private val patrolAi = PatrolAi(helmAi)

    @Test
    fun `sets throttle to 50 initially`() {
        executeAi()

        expectThat(ship.throttle)
            .isEqualTo(50)
    }

    @Test
    fun `sets course to first patrol point initially`() {
        executeAi()

        expectThat(patrolAi.pointInPath)
            .isEqualTo(0)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(0.0)
    }

    @Test
    fun `skips to next patrol point when point reached`() {
        executeAi()
        ship.position = p(1000, 0)
        helmAi.targetRotation = null
        executeAi()

        expectThat(patrolAi.pointInPath)
            .isEqualTo(1)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(PI * 0.5)
    }

    @Test
    fun `skips to first patrol point when last point reached`() {
        executeAi()
        ship.position = p(0, 0)
        helmAi.targetRotation = null
        patrolAi.pointInPath = 3
        executeAi()

        expectThat(patrolAi.pointInPath)
            .isEqualTo(0)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(0.0)
    }

    private fun executeAi() {
        patrolAi.execute(ship, time, ShipContactList(ship, emptyList()))
    }
}
