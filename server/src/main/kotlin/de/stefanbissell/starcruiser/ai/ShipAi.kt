package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip

class ShipAi(
    private val interval: Double = 1.0
) {

    private var lastCheck: Double = -Double.MAX_VALUE

    fun update(time: GameTime, ship: NonPlayerShip) {
        if (time.current - lastCheck > interval) {
            lastCheck = time.current
            execute(ship)
        }
    }

    private fun execute(ship: NonPlayerShip) {
        with(ship.shieldHandler) {
            if (!up && activationAllowed()) {
                up = true
            }
        }
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
