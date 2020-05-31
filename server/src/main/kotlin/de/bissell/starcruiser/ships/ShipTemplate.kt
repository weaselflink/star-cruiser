package de.bissell.starcruiser.ships

data class ShipTemplate(
    val className: String = "Infector",
    val throttleResponsiveness: Double = 25.0,
    val aheadThrustFactor: Double = 0.3,
    val reverseThrustFactor: Double = 0.1,
    val rudderFactor: Double = 0.4,
    val shieldRadius: Double = 10.0,
    val density: Double = 0.02
)
