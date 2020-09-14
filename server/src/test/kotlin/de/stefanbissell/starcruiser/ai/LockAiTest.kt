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
import java.time.Instant

class LockAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime(Instant.EPOCH)
    private val lockAi = LockAi()

    private val shipList = mutableListOf<Ship>()

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
    fun `locks hostile ship`() {
        val target = addShip()
        executeAi()

        expectLockStarted(target.id)
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
        position: Vector2 = p(100, 100),
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
