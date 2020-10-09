package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.interceptPoint
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class HomingAi(
    private val helmAi: HelmAi
) : ComponentAi(2.0) {

    var target: ObjectId? = null

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        selectTarget(contactList)
        target?.let {
            contactList[it]
        }?.also {
            steerTowardsTarget(it, ship)
        } ?: run {
            ship.throttle = 50
        }
    }

    override fun targetDestroyed(shipId: ObjectId) {
        if (target == shipId) {
            target = null
        }
    }

    private fun steerTowardsTarget(
        it: ShipContactList.ShipContact,
        ship: NonPlayerShip
    ) {
        if (it.range >= 100.0) {
            ship.throttle = 50
        } else {
            ship.throttle = 0
        }
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = angleToTarget(ship, it)
        }
    }

    private fun angleToTarget(
        ship: NonPlayerShip,
        targetShip: ShipContactList.ShipContact
    ): Double =
        if (targetShip.range > 200.0) {
            interceptPoint(
                interceptorPosition = ship.position,
                interceptorSpeed = ship.speed.length(),
                targetPosition = targetShip.position,
                targetSpeed = targetShip.speed
            )?.let {
                it - ship.position
            }?.angle() ?: targetShip.relativePosition.angle()
        } else {
            targetShip.relativePosition.angle()
        }

    private fun selectTarget(contactList: ShipContactList) {
        clearInvalidTarget(contactList)
        if (target == null) {
            target = contactList.allInSensorRange()
                .filter {
                    it.contactType == ContactType.Enemy
                }.minByOrNull {
                    it.range
                }?.id
        }
    }

    private fun clearInvalidTarget(contactList: ShipContactList) {
        target?.let {
            contactList[it]
        }.also {
            if (it == null || !it.inSensorRange) {
                target = null
            }
        }
    }
}
