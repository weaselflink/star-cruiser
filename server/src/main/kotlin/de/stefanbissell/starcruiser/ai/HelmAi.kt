package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.physics.PerformanceAnalysisStore
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList
import de.stefanbissell.starcruiser.smallestSignedAngleBetween
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.abs
import kotlin.math.sign

class HelmAi(interval: Double = 0.1) : ComponentAi(interval) {

    var targetRotation: Double? = 0.0
    var rudderNeutralPoint: Double? = null

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        targetRotation?.let {
            val diff = smallestSignedAngleBetween(ship.rotation, it)
            if (abs(diff) < 0.2.toRadians()) {
                endTurn(ship)
            } else {
                turnTowardsTarget(ship, diff)
            }
        }
    }

    private fun turnTowardsTarget(ship: NonPlayerShip, diff: Double) {
        calculateRudderNeutralPoint(ship, diff)

        rudderNeutralPoint?.also { point ->
            if (abs(diff) < point) {
                ship.rudder = 0
            } else {
                ship.rudder = (sign(diff) * 100).toInt()
            }
        }
    }

    private fun calculateRudderNeutralPoint(ship: NonPlayerShip, diff: Double) {
        if (rudderNeutralPoint == null) {
            PerformanceAnalysisStore[ship.template.className]?.also { performance ->
                val turn = performance.calculateTurn(abs(diff))
                rudderNeutralPoint = abs(diff) - turn
            }
        }
    }

    private fun endTurn(ship: NonPlayerShip) {
        ship.rudder = 0
        targetRotation = null
    }
}
