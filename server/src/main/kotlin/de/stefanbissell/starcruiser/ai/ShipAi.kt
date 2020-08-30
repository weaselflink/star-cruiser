package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip

class ShipAi {

    private val componentAis = listOf(
        ShieldAi(),
        RepairAi()
    )

    fun update(time: GameTime, ship: NonPlayerShip) {
        componentAis.forEach {
            it.update(time, ship)
        }
    }
}

abstract class ComponentAi(
    private val interval: Double = 1.0
) {

    private var lastCheck: Double = -Double.MAX_VALUE

    fun update(time: GameTime, ship: NonPlayerShip) {
        if (time.current - lastCheck > interval) {
            lastCheck = time.current
            execute(time, ship)
        }
    }

    abstract fun execute(time: GameTime, ship: NonPlayerShip)
}

class ShieldAi : ComponentAi() {

    override fun execute(time: GameTime, ship: NonPlayerShip) {
        with(ship.shieldHandler) {
            if (!up && activationAllowed()) {
                up = true
            }
        }
    }
}

class RepairAi : ComponentAi() {

    override fun execute(time: GameTime, ship: NonPlayerShip) {
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
