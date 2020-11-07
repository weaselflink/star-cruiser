package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.fullCircle
import de.stefanbissell.starcruiser.randomAngle
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ShipAi(
    val ship: NonPlayerShip
) {

    private val orders = sequence {
        yield(Order.Patrol(createInitialPatrolPath(ship)))
        if (ship.faction.enemies.isNotEmpty()) {
            yield(Order.SeekAndDestroy)
        }
    }.toList()

    private val behaviourAi = BehaviourAi()
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
        val aiState = AiState(
            ship = ship,
            time = time,
            contactList = contactList,
            orders = orders
        )
        componentAis.forEach {
            it.update(aiState)
        }
    }

    fun targetDestroyed(shipId: ObjectId) {
        componentAis.forEach {
            it.targetDestroyed(shipId)
        }
    }

    private fun createInitialPatrolPath(ship: NonPlayerShip): List<Vector2> {
        val angle = randomAngle()
        return listOf(
            ship.position + Vector2(1000, 0).rotate(angle),
            ship.position + Vector2(1000, 0).rotate(angle + fullCircle / 3),
            ship.position
        )
    }
}
