package de.stefanbissell.starcruiser.ships.combat

data class TorpedoTemplate(
    val model: String = "torpedo01",
    val radius: Double = 1.0,
    val mass: Double = 100.0,
    val thrust: Double = 2_000.0,
    val maxBurnTime: Double = 20.0,
    val damage: Double = 4.0
)
