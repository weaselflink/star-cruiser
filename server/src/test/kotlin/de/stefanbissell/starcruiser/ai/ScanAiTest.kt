package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipContactList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.time.Instant

class ScanAiTest {

    private val ship = NonPlayerShip()
    private val time = GameTime(Instant.EPOCH)
    private val scanAi = ScanAi()

    private var contactList = emptyList<Ship>()

    @Test
    fun `does not start scan before interval expired`() {
        updateAi()
        val target = NonPlayerShip().apply { position = p(100, 100) }
        contactList = listOf(target)
        time.update(4.9)
        updateAi()

        expectNoScanStarted()

        time.update(0.2)
        updateAi()

        expectScanStarted(target.id)
    }

    @Test
    fun `does not interrupt scan in progress`() {
        val target = NonPlayerShip().apply { position = p(100, 100) }
        contactList = listOf(target)
        val dummyId = ObjectId.random()
        ship.startScan(dummyId)
        executeAi()

        expectScanStarted(dummyId)
    }

    @Test
    fun `scans until detailed scan`() {
        val target = NonPlayerShip().apply { position = p(100, 100) }
        contactList = listOf(target)
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
        val farTarget = NonPlayerShip().apply { position = p(200, 200) }
        val invalidTarget = NonPlayerShip().apply { position = p(50, 50) }
        ship.scans[invalidTarget.id] = ScanLevel.Detailed
        val nearTarget = NonPlayerShip().apply { position = p(100, 100) }
        contactList = listOf(farTarget, nearTarget)
        executeAi()

        expectScanStarted(nearTarget.id)
    }

    @Test
    fun `does not scan targets out of range`() {
        val target = NonPlayerShip().apply { position = p(ship.template.sensorRange * 2.0, 0.0) }
        contactList = listOf(target)
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
        scanAi.update(ship, time, ShipContactList(ship, contactList))
    }

    private fun executeAi() {
        scanAi.execute(ship, time, ShipContactList(ship, contactList))
    }
}
