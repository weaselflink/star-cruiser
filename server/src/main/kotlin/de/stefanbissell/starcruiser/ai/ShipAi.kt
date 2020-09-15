package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ShipAi {

    private val componentAis = listOf(
        ShieldAi(),
        RepairAi(),
        ScanAi(),
        LockAi()
    )

    fun update(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        componentAis.forEach {
            it.update(
                ship = ship,
                time = time,
                contactList = contactList
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
        contactList: ShipContactList
    ) {
        if (time.current - lastCheck > interval) {
            lastCheck = time.current
            execute(
                ship = ship,
                time = time,
                contactList = contactList
            )
        }
    }

    abstract fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    )
}
