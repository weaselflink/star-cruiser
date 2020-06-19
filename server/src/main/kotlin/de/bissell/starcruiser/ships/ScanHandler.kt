package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.ObjectId
import de.bissell.starcruiser.ScanProgress

class ScanHandler(
    val targetId: ObjectId,
    private val scanningSpeed: Double
) {

    private var progress: Double = 0.0

    val isComplete: Boolean
        get() = progress >= 1.0

    fun update(time: GameTime) {
        progress += time.delta * scanningSpeed
    }

    fun toMessage() =
        ScanProgress(
            targetId = targetId,
            progress = progress
        )
}
