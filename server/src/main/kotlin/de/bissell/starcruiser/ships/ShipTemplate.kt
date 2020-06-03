package de.bissell.starcruiser.ships

import de.bissell.starcruiser.BeamMessage
import de.bissell.starcruiser.Vector3

data class ShipTemplate(
    val className: String = "Infector",
    val throttleResponsiveness: Double = 25.0,
    val aheadThrustFactor: Double = 0.3,
    val reverseThrustFactor: Double = 0.1,
    val rudderFactor: Double = 0.4,
    val shieldRadius: Double = 10.0,
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
    )
)

data class BeamWeapon(
    val position: Vector3 = Vector3(),
    val range: IntRange = 25..200,
    var leftArc: Int = 45,
    var rightArc: Int = -45
) {
    fun toMessage() =
        BeamMessage(
            position = position,
            minRange = range.first.toDouble(),
            maxRange = range.last.toDouble(),
            leftArc = leftArc.toDouble(),
            rightArc = rightArc.toDouble()
        )
}
