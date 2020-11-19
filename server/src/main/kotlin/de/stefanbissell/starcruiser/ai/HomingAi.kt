package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.interceptPoint
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.onlyEnemies
import de.stefanbissell.starcruiser.ships.onlyVessels

class HomingAi(
    private val behaviourAi: BehaviourAi,
    private val helmAi: HelmAi
) : ComponentAi(2.0) {

    var target: ObjectId? = null

    override fun execute(aiState: AiState) {
        if (behaviourAi.behaviour == Behaviour.Attack) {
            aiState.performAttack()
        }
    }

    override fun targetDestroyed(shipId: ObjectId) {
        if (target == shipId) {
            target = null
        }
    }

    private fun AiState.performAttack() {
        selectTarget(contactList)
        target?.let {
            contactList[it]
        }?.also {
            steerTowardsTarget(it, ship)
        } ?: run {
            ship.throttle = 50
        }
    }

    private fun steerTowardsTarget(
        it: ContactList.Contact,
        ship: NonPlayerShip
    ) {
        if (it.range >= 100.0) {
            ship.throttle = 70
        } else {
            ship.throttle = 0
        }
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = angleToTarget(ship, it)
        }
    }

    private fun angleToTarget(
        ship: NonPlayerShip,
        target: ContactList.Contact
    ): Double =
        if (target.range > 200.0) {
            interceptPoint(
                interceptorPosition = ship.position,
                interceptorSpeed = ship.speed.length(),
                targetPosition = target.position,
                targetSpeed = target.speed
            )?.let {
                it - ship.position
            }?.angle() ?: target.relativePosition.angle()
        } else {
            target.relativePosition.angle()
        }

    private fun selectTarget(contactList: ContactList) {
        clearInvalidTarget(contactList)
        if (target == null) {
            selectNewTarget(contactList)
        }
    }

    private fun selectNewTarget(contactList: ContactList) {
        target = contactList.allInSensorRange()
            .onlyVessels()
            .onlyEnemies()
            .minByOrNull {
                it.range
            }?.id
    }

    private fun clearInvalidTarget(contactList: ContactList) {
        target?.let {
            contactList[it]
        }.also {
            if (it == null || !it.inSensorRange) {
                target = null
            }
        }
    }
}
