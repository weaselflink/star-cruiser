package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ScanAi(interval: Double = 5.0) : ComponentAi(interval) {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        if (ship.scanHandler == null) {
            contactList.selectScanTarget()?.also {
                ship.startScan(it.id)
            }
        }
    }

    private fun ShipContactList.selectScanTarget(): ShipContactList.ShipContact? {
        return allInSensorRange()
            .filter {
                it.scanLevel.canBeIncreased
            }
            .minByOrNull {
                it.range
            }
    }
}
