package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

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

    open fun targetDestroyed(shipId: ObjectId) = Unit

    abstract fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    )
}
