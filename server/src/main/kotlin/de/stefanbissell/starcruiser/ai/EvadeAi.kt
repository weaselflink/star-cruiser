package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.fullCircle
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.onlyVessels
import kotlin.math.PI

class EvadeAi(
    private val behaviourAi: BehaviourAi,
    private val helmAi: HelmAi
) : ComponentAi(2.0) {

    var threat: ObjectId? = null

    override fun execute(aiState: AiState) {
        if (behaviourAi.behaviour is Behaviour.Evade) {
            performEvade(aiState.ship, aiState.contactList)
        }
    }

    private fun performEvade(
        ship: NonPlayerShip,
        contactList: ContactList
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
        contact: ContactList.Contact
    ) {
        ship.throttle = 70
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = angleAwayFromTarget(contact)
        }
    }

    private fun angleAwayFromTarget(
        target: ContactList.Contact
    ): Double =
        (target.relativePosition.angle() + PI) % fullCircle

    private fun ContactList.selectThreat() {
        val (enemies, rest) = allInSensorRange()
            .onlyVessels()
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
