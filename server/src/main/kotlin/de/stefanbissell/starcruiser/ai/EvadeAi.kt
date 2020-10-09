package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList
import kotlin.math.PI

class EvadeAi(
    private val behaviourAi: BehaviourAi,
    private val helmAi: HelmAi
) : ComponentAi(2.0) {

    var threat: ObjectId? = null

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        if (behaviourAi.behaviour is Behaviour.Evade) {
            performEvade(ship, contactList)
        }
    }

    private fun performEvade(
        ship: NonPlayerShip,
        contactList: ShipContactList
    ) {
        selectThreat(contactList)
        threat?.let {
            contactList[it]
        }?.also {
            steerClearOfThreat(ship, it)
        }
    }

    private fun steerClearOfThreat(
        ship: NonPlayerShip,
        it: ShipContactList.ShipContact
    ) {
        ship.throttle = 50
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = angleAwayFromTarget(it)
        }
    }

    private fun angleAwayFromTarget(
        targetShip: ShipContactList.ShipContact
    ): Double =
        (targetShip.relativePosition.angle() + PI) % (2 * PI)

    private fun selectThreat(contactList: ShipContactList) {
        clearInvalidThreat(contactList)
        threat = contactList.allInSensorRange()
            .filter {
                it.contactType != ContactType.Friendly
            }.minByOrNull {
                it.range
            }?.id
    }

    private fun clearInvalidThreat(contactList: ShipContactList) {
        threat?.let {
            contactList[it]
        }.also {
            if (it == null || !it.inSensorRange) {
                threat = null
            }
        }
    }
}
