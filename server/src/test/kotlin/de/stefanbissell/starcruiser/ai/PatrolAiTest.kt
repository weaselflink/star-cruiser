package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class PatrolAiTest {

    private val ship = NonPlayerShip(faction = TestFactions.neutral)
    private val time = GameTime.atEpoch()
    private val behaviourAi = BehaviourAi(Behaviour.CombatPatrol)
    private val helmAi = HelmAi()
    private val patrolPath = listOf(p(100, 0), p(100, 100), p(0, 0))
    private val patrolAi = PatrolAi(behaviourAi, helmAi, patrolPath)

    @Test
    fun `sets throttle to 50 initially`() {
        executeAi()

        expectThat(ship.throttle)
            .isEqualTo(50)
    }

    @Test
    fun `does nothing if behaviour not patrol`() {
        behaviourAi.behaviour = Behaviour.Attack

        executeAi()

        expectThat(ship.throttle)
            .isEqualTo(0)
    }

    @Test
    fun `sets course to first patrol point initially`() {
        executeAi()

        expectThat(patrolAi.pointInPath)
            .isEqualTo(0)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(patrolPath.first().angle())
    }

    @Test
    fun `skips to next patrol point when point reached`() {
        executeAi()
        ship.position = patrolPath.first()
        helmAi.targetRotation = null
        executeAi()

        expectThat(patrolAi.pointInPath)
            .isEqualTo(1)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear((patrolPath[1] - patrolPath.first()).angle())
    }

    @Test
    fun `skips to first patrol point when last point reached`() {
        executeAi()
        ship.position = patrolPath.last()
        helmAi.targetRotation = null
        patrolAi.pointInPath = patrolPath.size - 1
        executeAi()

        expectThat(patrolAi.pointInPath)
            .isEqualTo(0)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear((patrolPath.first() - patrolPath.last()).angle())
    }

    private fun executeAi() {
        patrolAi.execute(ship, time, ShipContactList(ship, emptyList()))
    }
}
