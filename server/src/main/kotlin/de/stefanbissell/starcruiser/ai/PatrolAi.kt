package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class PatrolAi(
    private val behaviourAi: BehaviourAi,
    private val helmAi: HelmAi,
    private val path: List<Vector2>
) : ComponentAi(2.0) {

    var pointInPath: Int = 0

    private val nextPoint: Vector2
        get() = path[pointInPath]

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        if (behaviourAi.behaviour is Behaviour.Patrol) {
            performPatrol(ship)
        }
    }

    private fun performPatrol(ship: NonPlayerShip) {
        if (path.isNotEmpty()) {
            with(ship) {
                checkPointReached()
                steerTowardsNextPoint()
            }
        }
    }

    private fun NonPlayerShip.checkPointReached() {
        if (nextPointRange < 50) {
            incrementNextPoint()
        }
    }

    private fun incrementNextPoint() {
        pointInPath = (pointInPath + 1) % path.size
    }

    private fun NonPlayerShip.steerTowardsNextPoint() {
        throttle = 50
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = nextPointAngle
        }
    }

    private val NonPlayerShip.nextPointRelative
        get() = nextPoint - position

    private val NonPlayerShip.nextPointRange
        get() = nextPointRelative.length()

    private val NonPlayerShip.nextPointAngle
        get() = nextPointRelative.angle()
}
