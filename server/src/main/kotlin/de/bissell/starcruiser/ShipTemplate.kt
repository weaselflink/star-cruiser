package de.bissell.starcruiser

data class ShipTemplate(
    val className: String = "Infector",
    val throttleResponsiveness: Double = 25.0,
    val aheadThrustFactor: Double = 0.2 * 5,
    val reverseThrustFactor: Double = 0.1 * 5,
    val rudderFactor: Double = 8.0 * 0.1
)
