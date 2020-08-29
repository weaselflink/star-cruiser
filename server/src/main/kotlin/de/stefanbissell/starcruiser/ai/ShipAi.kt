package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip

class ShipAi {

    fun update(time: GameTime, ship: NonPlayerShip) {
        with(ship.shieldHandler) {
            if (!up && activationAllowed()) {
                up = true
            }
        }
    }
}
