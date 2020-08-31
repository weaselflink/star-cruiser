package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipProvider

class RepairAi : ComponentAi() {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: List<Ship>,
        shipProvider: ShipProvider
    ) {
        with(ship.powerHandler) {
            if (!repairing) {
                poweredSystems.entries
                    .firstOrNull { it.value.canRepair() }
                    ?.also {
                        startRepair(it.key)
                    }
            }
        }
    }
}
