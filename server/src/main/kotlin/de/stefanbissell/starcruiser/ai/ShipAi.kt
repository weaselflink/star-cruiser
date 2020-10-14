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

    private val patrolPath = createInitialPatrolPath(ship)

    private val behaviourAi = if (ship.faction.enemies.isNotEmpty()) {
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
        PatrolAi(behaviourAi, helmAi, patrolPath),
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

    private fun createInitialPatrolPath(ship: NonPlayerShip): List<Vector2> {
        val angle = randomAngle()
        return listOf(
            ship.position + Vector2(1000, 0).rotate(angle),
            ship.position + Vector2(1000, 0).rotate(angle + fullCircle / 3),
            ship.position
        )
    }
}
