package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class LockAi(interval: Double = 5.0) : ComponentAi(interval) {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        if (ship.lockHandler == null) {
            contactList.selectLockTarget()?.also {
                ship.startLock(it.id)
            }
        }
    }

    private fun ShipContactList.selectLockTarget() =
        allInSensorRange()
            .filter {
                it.contactType == ContactType.Enemy
            }
            .minByOrNull { it.range }
}
