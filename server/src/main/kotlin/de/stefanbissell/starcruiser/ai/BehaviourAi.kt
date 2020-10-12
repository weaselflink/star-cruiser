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

    interface Evade

    fun NonPlayerShip.shieldsLow() =
        shieldHandler.currentStrength < template.shield.failureStrength * 2

    fun NonPlayerShip.shieldsHigh() =
        shieldHandler.currentStrength > template.shield.strength * 0.6

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
            if (ship.shieldHandler.timeSinceLastDamage < 10) {
                IdleEvade
            } else {
                IdlePatrol
            }
    }

    object IdleEvade : Behaviour(), Evade {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            if (ship.shieldHandler.timeSinceLastDamage > 30) {
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
            when {
                ship.shieldsLow() -> CombatEvade
                contactList.enemyInRange() -> Attack
                else -> CombatPatrol
            }

        private fun ShipContactList.enemyInRange() =
            allInSensorRange().any { it.isEnemy }
    }

    object Attack : Behaviour() {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            when {
                ship.shieldsLow() -> CombatEvade
                contactList.noEnemyInRange() -> CombatPatrol
                else -> Attack
            }

        private fun ShipContactList.noEnemyInRange() =
            allInSensorRange().none { it.isEnemy }
    }

    object CombatEvade : Behaviour(), Evade {

        override fun transition(
            ship: NonPlayerShip,
            time: GameTime,
            contactList: ShipContactList
        ): Behaviour =
            if (ship.shieldsHigh()) {
                CombatPatrol
            } else {
                CombatEvade
            }
    }
}
