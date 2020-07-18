package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.Vector3
import de.stefanbissell.starcruiser.toRadians
import kotlin.math.roundToInt

data class ShipTemplate(
    val className: String = "Infector",
    val throttleResponsiveness: Double = 25.0,
    val aheadThrustFactor: Double = 0.3,
    val reverseThrustFactor: Double = 0.1,
    val rudderFactor: Double = 0.4,
    val density: Double = 0.02,
    val scanSpeed: Double = 0.2,
    val lockingSpeed: Double = 0.5,
    val shortRangeScopeRange: Double = 400.0,
    val beams: List<BeamWeapon> = listOf(
        BeamWeapon(
            position = Vector3(
                x = -1.9,
                y = -4.8,
                z = -12.2
            ),
            rightArc = -10
        ),
        BeamWeapon(
            position = Vector3(
                x = +1.9,
                y = -4.8,
                z = -12.2
            ),
            leftArc = 10
        )
    ),
    val shield: ShieldTemplate = ShieldTemplate(),
    val hull: Double = 10.0,
    val jumpDrive: JumpDrive = JumpDrive(),
    val poweredSystemDamageCapacity: Double = 5.0,
    val maxCapacitors: Double = 1000.0,
    val reactorOutput: Double = 300.0,
    val maxCoolant: Double = 2.0,
    val heatDamage: Double = 2.0,
    val repairSpeed: Double = 0.1,
    val repairAmount: Double = 0.25
)

data class JumpDrive(
    val minDistance: Int = 1_000,
    val maxDistance: Int = 11_000,
    val increment: Int = 500,
    val jumpingSpeed: Double = 0.2,
    val rechargeSpeed: Double = 0.05
) {

    private val distanceRange: Int
        get() = maxDistance - minDistance

    fun ratioToDistance(ratio: Double): Int =
        ((distanceRange * ratio + minDistance) / increment).roundToInt() * increment

    fun distanceToRatio(distance: Int): Double =
        (distance - minDistance).toDouble() / distanceRange
}

data class BeamWeapon(
    val position: Vector3 = Vector3(),
    val range: IntRange = 25..200,
    val leftArc: Int = 45,
    val rightArc: Int = -45,
    val rechargeSpeed: Double = 0.2,
    val firingSpeed: Double = 1.0
) {

    fun isInRange(relativePosition: Vector2): Boolean {
        val pos = relativePosition - Vector2(-position.z, position.x)
        val distance = pos.length()
        if (distance < range.first || distance > range.last) {
            return false
        }
        val angle = pos.angle()
        return angle <= leftArc.toRadians() && angle >= rightArc.toRadians()
    }
}

data class ShieldTemplate(
    val radius: Double = 17.0,
    val strength: Double = 10.0,
    val rechargeSpeed: Double = 0.1,
    val failureStrength: Double = 0.5,
    val activationStrength: Double = 2.0
)
