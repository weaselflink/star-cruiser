package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.onlyVessels

class BehaviourAi(
    var behaviour: Behaviour = Behaviour.Idle
) : ComponentAi() {

    override fun execute(aiState: AiState) {
        behaviour = behaviour.transition(aiState)
    }
}

sealed class Behaviour {

    interface Patrol

    interface Evade

    fun NonPlayerShip.shieldsLow() =
        shieldHandler.currentStrength < template.shield.failureStrength * 2

    fun NonPlayerShip.shieldsHigh() =
        shieldHandler.currentStrength > template.shield.strength * 0.6

    open fun transition(aiState: AiState): Behaviour = this

    object Idle : Behaviour() {

        override fun transition(aiState: AiState): Behaviour =
            if (aiState.orders.contains(Order.SeekAndDestroy)) {
                CombatPatrol
            } else {
                PeacefulPatrol
            }
    }

    object PeacefulPatrol : Behaviour(), Patrol {

        override fun transition(aiState: AiState): Behaviour =
            if (aiState.ship.shieldHandler.timeSinceLastDamage < 10) {
                PeacefulEvade
            } else {
                PeacefulPatrol
            }
    }

    object PeacefulEvade : Behaviour(), Evade {

        override fun transition(aiState: AiState): Behaviour =
            if (aiState.ship.shieldHandler.timeSinceLastDamage > 30) {
                PeacefulPatrol
            } else {
                PeacefulEvade
            }
    }

    object CombatPatrol : Behaviour(), Patrol {

        override fun transition(aiState: AiState): Behaviour =
            when {
                aiState.ship.shieldsLow() -> CombatEvade
                aiState.contactList.enemyInRange() -> Attack
                else -> CombatPatrol
            }

        private fun ContactList.enemyInRange() =
            allInSensorRange()
                .onlyVessels()
                .any { it.isEnemy }
    }

    object Attack : Behaviour() {

        override fun transition(aiState: AiState): Behaviour =
            when {
                aiState.ship.shieldsLow() -> CombatEvade
                aiState.contactList.noEnemyInRange() -> CombatPatrol
                else -> Attack
            }

        private fun ContactList.noEnemyInRange() =
            allInSensorRange()
                .onlyVessels()
                .none { it.isEnemy }
    }

    object CombatEvade : Behaviour(), Evade {

        override fun transition(aiState: AiState): Behaviour =
            if (aiState.ship.shieldsHigh()) {
                CombatPatrol
            } else {
                CombatEvade
            }
    }
}
