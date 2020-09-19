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

    private val tolerance = 0.2.toRadians()

    var targetRotation: Double? = null
    var rudderNeutralPoint: Double? = null
    var endTurnCondition: (Double) -> Boolean = { false }

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        targetRotation?.let {
            if (endTurnCondition(ship.rotation)) {
                endTurn(ship)
            } else {
                val diff = smallestSignedAngleBetween(ship.rotation, it)
                turnTowardsTarget(ship, diff)
            }
        }
    }

    private fun turnTowardsTarget(ship: NonPlayerShip, diff: Double) {
        calculateRudderNeutralPoint(ship, diff)

        rudderNeutralPoint?.also { point ->
            if (abs(diff) < point) {
                ship.rudder = (diff.sign * 10).toInt()
            } else {
                ship.rudder = (diff.sign * 100).toInt()
            }
        }
    }

    private fun calculateRudderNeutralPoint(ship: NonPlayerShip, diff: Double) {
        if (rudderNeutralPoint == null) {
            PerformanceAnalysisStore[ship.template.className]?.also { performance ->
                val turn = performance.calculateTurn(abs(diff))
                rudderNeutralPoint = abs(diff) - turn
                endTurnCondition = if (diff.sign > 0) {
                    { it > targetRotation!! - tolerance }
                } else {
                    { it < targetRotation!! + tolerance }
                }
            }
        }
    }

    private fun endTurn(ship: NonPlayerShip) {
        ship.rudder = 0
        targetRotation = null
        rudderNeutralPoint = null
        endTurnCondition = { false }
    }
}
