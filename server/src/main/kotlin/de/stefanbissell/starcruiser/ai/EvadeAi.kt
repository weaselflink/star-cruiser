package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.fullCircle
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
        contactList.selectThreat()
        threat?.let {
            contactList[it]
        }?.also {
            steerClearOfThreat(ship, it)
        }
    }

    private fun steerClearOfThreat(
        ship: NonPlayerShip,
        contact: ShipContactList.ShipContact
    ) {
        ship.throttle = 70
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = angleAwayFromTarget(contact)
        }
    }

    private fun angleAwayFromTarget(
        targetShip: ShipContactList.ShipContact
    ): Double =
        (targetShip.relativePosition.angle() + PI) % fullCircle

    private fun ShipContactList.selectThreat() {
        val (enemies, rest) = allInSensorRange()
            .filter {
                it.contactType != ContactType.Friendly
            }
            .partition {
                it.contactType == ContactType.Enemy
            }
        val contact = enemies.minByOrNull {
            it.range
        } ?: rest.minByOrNull {
            it.range
        }
        threat = contact?.id
    }
}
