package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipContactList

class ScanAi(interval: Double = 5.0) : ComponentAi(interval) {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        if (ship.scanHandler == null) {
            selectScanTarget(ship, contactList)?.also {
                ship.startScan(it.id)
            }
        }
    }

    private fun selectScanTarget(ship: NonPlayerShip, contactList: ShipContactList): ShipContactList.ShipContact? {
        return contactList
            .allInSensorRange()
            .filter {
                scanLevel(ship, it.ship).canBeIncreased
            }
            .minByOrNull {
                (it.position - ship.position).length()
            }
    }

    private fun scanLevel(ship: NonPlayerShip, it: Ship) =
        ship.scans[it.id] ?: ScanLevel.None
}
