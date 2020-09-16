package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.Vector2
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

class LockAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime.atEpoch()
    private val lockAi = LockAi()

    private val shipList = mutableListOf<Ship>()
    private val halfScopeRange = ship.template.shortRangeScopeRange * 0.5
    private val doubleScopeRange = ship.template.shortRangeScopeRange * 2

    @Test
    fun `does not start lock before interval expired`() {
        updateAi()
        val target = addShip()
        time.update(4.9)
        updateAi()

        expectNoLockStarted()

        time.update(0.2)
        updateAi()

        expectLockStarted(target.id)
    }

    @Test
    fun `does not interrupt scan in progress`() {
        addShip()
        val dummyId = ObjectId.random()
        ship.startLock(dummyId)
        executeAi()

        expectLockStarted(dummyId)
    }

    @Test
    fun `locks hostile in scope range ship`() {
        val target = addShip()
        executeAi()

        expectLockStarted(target.id)
    }

    @Test
    fun `does not lock hostile outside scope range ship`() {
        addShip(p(doubleScopeRange, 0))
        executeAi()

        expectNoLockStarted()
    }

    @Test
    fun `does not lock friendly ship`() {
        addShip(hostile = false)
        executeAi()

        expectNoLockStarted()
    }

    @Test
    fun `locks nearest hostile ship`() {
        addShip(p(200, 200))
        val nearTarget = addShip(p(100, 100))
        executeAi()

        expectLockStarted(nearTarget.id)
    }

    @Test
    fun `locks onto another target if current is out of scope range`() {
        val targetMovingAway = addShip(p(halfScopeRange, 0))
        val targetApproaching = addShip(p(doubleScopeRange, 0))
        executeAi()

        expectLockStarted(targetMovingAway.id)

        targetMovingAway.position = p(doubleScopeRange, 0)
        targetApproaching.position = p(halfScopeRange, 0)
        executeAi()

        expectLockStarted(targetApproaching.id)
    }

    private fun expectLockStarted(id: ObjectId) {
        expectThat(ship.lockHandler)
            .isNotNull()
            .get { targetId }.isEqualTo(id)
    }

    private fun expectNoLockStarted() {
        expectThat(ship.lockHandler)
            .isNull()
    }

    private fun updateAi() {
        lockAi.update(ship, time, ShipContactList(ship, shipList))
    }

    private fun executeAi() {
        lockAi.execute(ship, time, ShipContactList(ship, shipList))
    }

    private fun addShip(
        position: Vector2 = p(halfScopeRange, 0),
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
