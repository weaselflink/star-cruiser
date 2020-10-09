package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class BehaviourAi(
    var behaviour: Behaviour
) : ComponentAi() {

    override fun execute(ship: NonPlayerShip, time: GameTime, contactList: ShipContactList) {
        behaviour = behaviour.transition(ship, time, contactList)
    }
}

sealed class Behaviour {

    interface Patrol

    open fun transition(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ): Behaviour = this

    object IdlePatrol : Behaviour(), Patrol {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            if (ship.shieldHandler.timeSinceActivation < 10) {
                IdleEvade
            } else {
                IdlePatrol
            }
    }

    object IdleEvade : Behaviour() {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            if (ship.shieldHandler.timeSinceActivation > 30) {
                IdlePatrol
            } else {
                IdleEvade
            }
    }

    object CombatPatrol : Behaviour(), Patrol {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            if (contactList.allInSensorRange().any { it.isEnemy }) {
                Attack
            } else {
                CombatPatrol
            }
    }

    object Attack : Behaviour() {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            if (contactList.allInSensorRange().none { it.isEnemy }) {
                CombatPatrol
            } else {
                Attack
            }
    }
}
