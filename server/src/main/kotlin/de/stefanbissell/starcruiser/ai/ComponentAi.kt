package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip

abstract class ComponentAi(
    private val interval: Double = 1.0
) {

    private var lastCheck: Double = -Double.MAX_VALUE

    fun update(aiState: AiState) {
        if (aiState.time.current - lastCheck > interval) {
            lastCheck = aiState.time.current
            execute(aiState)
        }
    }

    open fun targetDestroyed(shipId: ObjectId) = Unit

    abstract fun execute(aiState: AiState)
}

data class AiState(
    val ship: NonPlayerShip,
    val time: GameTime,
    val contactList: ContactList,
    val orders: List<Order> = emptyList()
)
