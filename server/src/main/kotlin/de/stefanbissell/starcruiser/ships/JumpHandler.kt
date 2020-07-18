package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.JumpDriveMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import kotlin.math.max
import kotlin.math.min

class JumpHandler(
    private val jumpDrive: JumpDrive
) {

    var jumping: Boolean = false
        private set
    var jumpDistance: Int = jumpDrive.minDistance
        private set
    private var jumpProgress: Double = 0.0
    private var rechargeProgress: Double = 1.0
    private var animation: Double? = null

    val ready: Boolean
        get() = rechargeProgress >= 1.0
    val jumpComplete: Boolean
        get() = jumping && jumpProgress >= 1.0

    fun update(time: GameTime, boostLevel: Double) {
        if (jumping) {
            jumpProgress += time.delta * jumpDrive.jumpingSpeed
        } else {
            rechargeProgress = min(1.0, rechargeProgress + time.delta * jumpDrive.rechargeSpeed * boostLevel)
        }
        updateAnimation(time)
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
        animation = 0.0
    }

    fun toMessage() =
        when {
            jumping -> JumpDriveMessage.Jumping(
                ratio = jumpDrive.distanceToRatio(jumpDistance).fiveDigits(),
                distance = jumpDistance,
                animation = animation?.fiveDigits(),
                progress = jumpProgress.fiveDigits()
            )
            rechargeProgress < 1.0 -> JumpDriveMessage.Recharging(
                ratio = jumpDrive.distanceToRatio(jumpDistance).fiveDigits(),
                distance = jumpDistance,
                animation = animation?.fiveDigits(),
                progress = rechargeProgress.fiveDigits()
            )
            else -> JumpDriveMessage.Ready(
                ratio = jumpDrive.distanceToRatio(jumpDistance).fiveDigits(),
                distance = jumpDistance,
                animation = animation?.fiveDigits()
            )
        }

    private fun updateAnimation(time: GameTime) {
        animation = if (jumping) {
            secondsToJump().unaryMinus().let {
                if (it < -1.0) null else it
            }
        } else {
            animation?.let {
                it + time.delta
            }?.let {
                if (it > 1.0) null else it
            }
        }
    }

    private fun secondsToJump() =
        max(0.0, ((1.0 - jumpProgress) / jumpDrive.jumpingSpeed))
}
