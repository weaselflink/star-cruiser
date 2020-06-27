package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.JumpDriveMessage
import de.bissell.starcruiser.clamp
import kotlin.math.min

class JumpHandler(
    private val jumpDrive: JumpDrive
) {

    private var jumping: Boolean = false
    private var jumpDistance: Int = jumpDrive.minDistance
    private var jumpProgress: Double = 0.0
    private var rechargeProgress: Double = 1.0

    fun update(time: GameTime) {
        if (jumping) {
            jumpProgress += time.delta * jumpDrive.jumpingSpeed
        } else {
            rechargeProgress = min(1.0, rechargeProgress + time.delta * jumpDrive.rechargeSpeed)
        }
    }

    fun changeJumpDistance(value: Double) {
        jumpDistance = jumpDrive.ratioToDistance(value.clamp(0.0, 1.0))
    }

    fun startJump() {
        jumping = true
        rechargeProgress = 0.0
    }

    fun endJump() {
        jumping = false
        jumpProgress = 0.0
    }

    fun toMessage() =
        JumpDriveMessage(
            ratio = jumpDrive.distanceToRatio(jumpDistance),
            distance = jumpDistance
        )
}
