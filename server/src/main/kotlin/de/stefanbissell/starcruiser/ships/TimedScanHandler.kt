package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId

class TimedScanHandler(
    val targetId: ObjectId,
    private val scanningSpeed: Double
) {

    private var progress: Double = 0.0

    val isComplete: Boolean
        get() = progress >= 1.0

    fun update(time: GameTime) {
        progress += time.delta * scanningSpeed
    }
}
