package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.interceptPoint
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class HomingAi(
    private val helmAi: HelmAi,
    interval: Double = 10.0
) : ComponentAi(interval) {

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
            if (helmAi.targetRotation == null) {
                helmAi.targetRotation = angleToTarget(ship, it)
            }
        }
    }

    private fun angleToTarget(
        ship: NonPlayerShip,
        targetShip: ShipContactList.ShipContact
    ): Double =
        interceptPoint(
            interceptorPosition = ship.position,
            interceptorSpeed = ship.speed.length(),
            targetPosition = targetShip.position,
            targetSpeed = targetShip.speed
        )?.let {
            it - ship.position
        }?.angle() ?: targetShip.relativePosition.angle()

    override fun targetDestroyed(shipId: ObjectId) {
        if (target == shipId) {
            target = null
        }
    }

    private fun selectTarget(contactList: ShipContactList) {
        if (target == null) {
            target = contactList.allInSensorRange()
                .filter {
                    it.contactType == ContactType.Enemy
                }.minByOrNull {
                    it.range
                }
                ?.id
        }
    }
}
