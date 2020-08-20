package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanProgressMessage
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.minigames.FrequencyGame

class ScanHandler(
    val targetId: ObjectId,
    private val boostLevel: BoostLevel
) {

    private val game = FrequencyGame.createUnsolved(2)
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

    fun adjustInput(dimension: Int, value: Double) {
        solvedTimer = 0.0
        game.adjustInput(dimension, value)
    }

    fun toMessage(shipProvider: (ObjectId) -> Ship?) =
        ScanProgressMessage(
            targetId = targetId,
            designation = shipProvider(targetId)?.designation ?: "unknown",
            noise = game.noise.fiveDigits(),
            input = game.input.map { it.fiveDigits() }
        )
}
