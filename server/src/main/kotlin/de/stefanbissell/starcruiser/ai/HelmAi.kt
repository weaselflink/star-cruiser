package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList
import de.stefanbissell.starcruiser.smallestSignedAngleBetween
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.abs
import kotlin.math.sign

class HelmAi(interval: Double = 0.1) : ComponentAi(interval) {

    var targetRotation: Double? = 0.0

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        targetRotation?.let {
            val diff = smallestSignedAngleBetween(ship.rotation, it)
            if (abs(diff) < 0.2.toRadians()) {
                ship.rudder = 0
                targetRotation = null
            } else {
                ship.rudder = (sign(diff) * 100).toInt()
            }
        }
    }
}
