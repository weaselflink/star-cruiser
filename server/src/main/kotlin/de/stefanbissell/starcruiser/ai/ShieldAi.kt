package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ShieldAi : ComponentAi() {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        with(ship.shieldHandler) {
            val hostile = contactList.closestHostile()
            if (hostile != null && ship.rangeTo(hostile.position) <= 500.0) {
                if (!up && activationAllowed()) {
                    up = true
                }
            } else {
                if (up) {
                    up = false
                }
            }
        }
    }

    private fun ShipContactList.closestHostile() =
        allInSensorRange()
            .firstOrNull {
                it.contactType == ContactType.Enemy
            }
}
