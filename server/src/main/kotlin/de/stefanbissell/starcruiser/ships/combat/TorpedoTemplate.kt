package de.stefanbissell.starcruiser.ships.combat

data class TorpedoTemplate(
    val model: String = "torpedo01",
    val radius: Double = 1.0,
    val drive: TorpedoDriveTemplate = TorpedoDriveTemplate(),
    val warhead: TorpedoWarheadTemplate = TorpedoWarheadTemplate()
) {

    val totalMass
        get() = drive.mass + warhead.mass
}

data class TorpedoWarheadTemplate(
    val name: String = "Basic",
    val mass: Double = 50.0,
    val damage: Double = 4.0
)

data class TorpedoDriveTemplate(
    val name: String = "Basic",
    val mass: Double = 50.0,
    val thrust: Double = 2_000.0,
    val maxBurnTime: Double = 20.0
)
