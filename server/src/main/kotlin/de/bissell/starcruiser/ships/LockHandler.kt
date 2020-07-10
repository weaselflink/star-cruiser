package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.ObjectId

class LockHandler(
    val targetId: ObjectId,
    private val lockingSpeed: Double
) {

    private var progress: Double = 0.0

    val isComplete: Boolean
        get() = progress >= 1.0

    fun update(time: GameTime) {
        progress += time.delta * lockingSpeed
    }

    fun toMessage(): LockStatus =
        if (isComplete) {
            LockStatus.Locked(targetId)
        } else {
            LockStatus.InProgress(targetId, progress)
        }
}
