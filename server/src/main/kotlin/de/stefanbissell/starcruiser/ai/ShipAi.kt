package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ships.Faction
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ShipAi(
    val ship: NonPlayerShip
) {

    private val helmAi = HelmAi()
    private val componentAis = listOf(
        ShieldAi(),
        RepairAi(),
        ScanAi(),
        LockAi(),
        helmAi,
    ).let {
        when (ship.faction) {
            Faction.Enemy -> it + HomingAi(helmAi)
            Faction.Neutral -> it + PatrolAi(helmAi)
            else -> it
        }
    }

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
