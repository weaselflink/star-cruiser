package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanProgressMessage
import de.stefanbissell.starcruiser.minigames.FrequencyGame

class ScanHandler(
    val targetId: ObjectId,
    private val boostLevel: BoostLevel
) {

    val game = FrequencyGame.createUnsolved(2)
    private var solvedTimer = 0.0

    val isComplete: Boolean
        get() = solvedTimer > 1.0

    fun update(time: GameTime) {
        if (game.isSolved) {
            solvedTimer += time.delta
        } else {
            solvedTimer = 0.0
        }
    }

    fun toMessage(shipProvider: (ObjectId) -> Ship?) =
        ScanProgressMessage(
            targetId = targetId,
            designation = shipProvider(targetId)?.designation ?: "unknown",
            noise = game.noise,
            input = game.input.toList()
        )
}
