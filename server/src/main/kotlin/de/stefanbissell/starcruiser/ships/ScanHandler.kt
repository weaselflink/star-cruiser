package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanProgressMessage
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.minigames.FrequencyGame
import kotlin.math.max
import kotlin.math.min

class ScanHandler(
    val targetId: ObjectId,
    private val boostLevel: BoostLevel
) {

    private val game = FrequencyGame.createUnsolved(2)
    private var solvedTimer = 0.0

    val isComplete: Boolean
        get() = solvedTimer > 1.0

    fun update(time: GameTime) {
        if (adjustedNoise() < 0.1) {
            solvedTimer += time.delta
        } else {
            solvedTimer = 0.0
        }
    }

    fun adjustInput(dimension: Int, value: Double) {
        solvedTimer = 0.0
        game.adjustInput(dimension, value)
    }

    fun toMessage(contactList: ShipContactList) =
        ScanProgressMessage(
            targetId = targetId,
            designation = contactList[targetId]?.designation ?: "unknown",
            noise = adjustedNoise().fiveDigits(),
            input = game.inputs.map { it.fiveDigits() }
        )

    private fun boostLevelFactor() = 1.0 / max(0.1, boostLevel())

    private fun adjustedNoise() = min(1.0, game.noise * boostLevelFactor())
}
