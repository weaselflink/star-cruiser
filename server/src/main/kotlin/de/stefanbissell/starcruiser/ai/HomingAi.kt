package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
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
        println("${ship.designation} target $target")
        target?.let {
            contactList[it]
        }?.also {
            if (helmAi.targetRotation == null) {
                println("${ship.designation} helm ${it.relativePosition.angle()}")
                helmAi.targetRotation = it.relativePosition.angle()
            }
        }
    }

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
