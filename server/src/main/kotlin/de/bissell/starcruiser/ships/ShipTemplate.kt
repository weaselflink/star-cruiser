package de.bissell.starcruiser.ships

import de.bissell.starcruiser.Vector2
import de.bissell.starcruiser.Vector3
import de.bissell.starcruiser.toRadians

data class ShipTemplate(
    val className: String = "Infector",
    val throttleResponsiveness: Double = 25.0,
    val aheadThrustFactor: Double = 0.3,
    val reverseThrustFactor: Double = 0.1,
    val rudderFactor: Double = 0.4,
    val shieldRadius: Double = 17.0,
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
    val shield: ShieldTemplate = ShieldTemplate()
)

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
    val strength: Double = 10.0,
    val rechargeSpeed: Double = 0.25
)
