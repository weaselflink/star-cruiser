package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.scenario.Faction
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class ScanAiTest {

    private val ship = NonPlayerShip(faction = TestFactions.enemy)
    private val time = GameTime.atEpoch()
    private val scanAi = ScanAi()

    private val shipList = mutableListOf<Ship>()

    @Test
    fun `does not start scan before interval expired`() {
        updateAi()
        val target = addShip()
        time.update(4.9)
        updateAi()

        expectNoScanStarted()

        time.update(0.2)
        updateAi()

        expectScanStarted(target.id)
    }

    @Test
    fun `does not interrupt scan in progress`() {
        addShip()
        val dummyId = ObjectId.random()
        ship.startScan(dummyId)
        executeAi()

        expectScanStarted(dummyId)
    }

    @Test
    fun `scans until detailed scan`() {
        val target = addShip()
        executeAi()

        expectScanStarted(target.id)

        finishScan(target.id)
        executeAi()

        expectScanStarted(target.id)

        finishScan(target.id)
        executeAi()

        expectNoScanStarted()
    }

    @Test
    fun `scans nearest possible target`() {
        addShip(p(200, 200))
        val invalidTarget = addShip(p(50, 50))
        ship.scans[invalidTarget.id] = ScanLevel.Detailed
        val nearTarget = addShip(p(100, 100))
        executeAi()

        expectScanStarted(nearTarget.id)
    }

    @Test
    fun `scans friendly targets last until detailed scan`() {
        val nonFriendlyTarget = addShip(p(200, 200))
        addShip(p(100, 100), faction = TestFactions.enemy)
        executeAi()

        expectScanStarted(nonFriendlyTarget.id)
    }

    @Test
    fun `scans friendlies after all other are detailed`() {
        val nonFriendlyTarget = addShip(p(200, 200))
        val friendly = addShip(p(100, 100), faction = TestFactions.enemy)
        ship.scans[nonFriendlyTarget.id] = ScanLevel.Detailed
        executeAi()

        expectScanStarted(friendly.id)
    }

    @Test
    fun `does not scan targets out of range`() {
        addShip(p(ship.template.sensorRange * 2.0, 0.0))
        executeAi()

        expectNoScanStarted()
    }

    private fun expectScanStarted(id: ObjectId) {
        expectThat(ship.scanHandler)
            .isNotNull()
            .get { targetId }.isEqualTo(id)
    }

    private fun expectNoScanStarted() {
        expectThat(ship.scanHandler)
            .isNull()
    }

    private fun finishScan(id: ObjectId) {
        ship.scans[id] = (ship.scans[id] ?: ScanLevel.None).next
        ship.scanHandler = null
    }

    private fun updateAi() {
        scanAi.update(ship, time, ShipContactList(ship, shipList))
    }

    private fun executeAi() {
        scanAi.execute(ship, time, ShipContactList(ship, shipList))
    }

    private fun addShip(
        position: Vector2 = p(100, 100),
        faction: Faction = TestFactions.player
    ): Ship =
        NonPlayerShip(faction = faction, position = position).also {
            shipList.add(it)
        }
}
