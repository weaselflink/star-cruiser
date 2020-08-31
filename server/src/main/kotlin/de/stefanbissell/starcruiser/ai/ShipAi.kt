package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipProvider

class ShipAi {

    private val componentAis = listOf(
        ShieldAi(),
        RepairAi(),
        ScanAi()
    )

    fun update(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: List<Ship>,
        shipProvider: ShipProvider
    ) {
        componentAis.forEach {
            it.update(
                ship = ship,
                time = time,
                contactList = contactList,
                shipProvider = shipProvider
            )
        }
    }
}

abstract class ComponentAi(
    private val interval: Double = 1.0
) {

    private var lastCheck: Double = -Double.MAX_VALUE

    fun update(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: List<Ship>,
        shipProvider: ShipProvider
    ) {
        if (time.current - lastCheck > interval) {
            lastCheck = time.current
            execute(
                ship = ship,
                time = time,
                contactList = contactList,
                shipProvider = shipProvider
            )
        }
    }

    abstract fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: List<Ship>,
        shipProvider: ShipProvider
    )
}

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
