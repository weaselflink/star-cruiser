package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipProvider

class ShieldAi : ComponentAi() {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: List<Ship>,
        shipProvider: ShipProvider
    ) {
        with(ship.shieldHandler) {
            if (!up && activationAllowed()) {
                up = true
            }
        }
    }
}
