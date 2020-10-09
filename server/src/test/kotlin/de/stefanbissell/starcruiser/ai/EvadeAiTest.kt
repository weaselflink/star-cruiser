package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.Faction
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import kotlin.math.PI

class EvadeAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime.atEpoch()
    private val behaviourAi = BehaviourAi(Behaviour.IdleEvade)
    private val helmAi = HelmAi()
    private val evadeAi = EvadeAi(behaviourAi, helmAi)

    private val shipList = mutableListOf<Ship>()

    @Test
    fun `throttles up when threat in sensor range`() {
        addShip()

        executeAi()

        expectThat(ship.throttle).isEqualTo(50)
    }

    @Test
    fun `does nothing if behaviour not evade`() {
        behaviourAi.behaviour = Behaviour.IdlePatrol

        executeAi()

        expectThat(ship.throttle).isEqualTo(0)
    }

    @Test
    fun `does nothing if no threat in sensor range`() {
        executeAi()

        expectThat(ship.throttle).isEqualTo(0)
    }

    @Test
    fun `does nothing if only friendlies in sensor range`() {
        addShip(hostile = false)

        executeAi()

        expectThat(ship.throttle).isEqualTo(0)
    }

    @Test
    fun `steers away from non-friendly ship`() {
        val target = addShip()

        executeAi()

        expectThat(evadeAi.threat).isEqualTo(target.id)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(PI + PI * 0.25)
    }

    @Test
    fun `steers away from closest non-friendly ship`() {
        addShip(p(1_000, 1_000))
        val nearTarget = addShip(p(500, -500))

        executeAi()

        expectThat(evadeAi.threat).isEqualTo(nearTarget.id)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(PI - PI * 0.25)
    }

    @Test
    fun `does not change helm if already turning`() {
        val target = addShip()
        helmAi.targetRotation = 0.0

        executeAi()

        expectThat(evadeAi.threat).isEqualTo(target.id)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(0.0)
    }

    @Test
    fun `looses target when no longer in contact list`() {
        evadeAi.threat = ObjectId.random()

        executeAi()

        expectThat(evadeAi.threat).isNull()
    }

    @Test
    fun `looses target when out of sensor range`() {
        val target = addShip(p(ship.sensorRange * 2, 0))
        evadeAi.threat = target.id

        executeAi()

        expectThat(evadeAi.threat).isNull()
    }

    private fun executeAi() {
        evadeAi.execute(ship, time, ShipContactList(ship, shipList))
    }

    private fun addShip(
        position: Vector2 = p(1_000, 1_000),
        hostile: Boolean = true
    ): Ship {
        val target = NonPlayerShip(
            position = position,
            faction = if (hostile) Faction.Player else Faction.Enemy
        )
        ship.scans[target.id] = ScanLevel.Detailed
        shipList.add(target)
        return target
    }
}
