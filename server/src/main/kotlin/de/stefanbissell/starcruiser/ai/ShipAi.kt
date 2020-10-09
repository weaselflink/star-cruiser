package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ships.Faction
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ShipAi(
    val ship: NonPlayerShip
) {

    private val behaviourAi = if (ship.faction == Faction.Enemy) {
        BehaviourAi(Behaviour.CombatPatrol)
    } else {
        BehaviourAi(Behaviour.IdlePatrol)
    }
    private val helmAi = HelmAi()
    private val componentAis = listOf(
        behaviourAi,
        ShieldAi(),
        RepairAi(),
        ScanAi(),
        LockAi(),
        helmAi,
        PatrolAi(behaviourAi, helmAi),
        HomingAi(behaviourAi, helmAi),
        EvadeAi(behaviourAi, helmAi)
    )

    fun update(
        time: GameTime,
        contactList: ShipContactList
    ) {
        componentAis.forEach {
            it.update(
                ship = ship,
                time = time,
                contactList = contactList
            )
        }
    }

    fun targetDestroyed(shipId: ObjectId) {
        componentAis.forEach {
            it.targetDestroyed(shipId)
        }
    }
}
