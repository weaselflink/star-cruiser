package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.LockStatus
import de.bissell.starcruiser.ObjectId

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
