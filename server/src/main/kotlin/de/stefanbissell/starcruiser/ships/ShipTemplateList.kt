package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.Vector3

val carrierTemplate = ShipTemplate()

val cruiserTemplate = ShipTemplate().copy(
    className = "Protector",
    model = "cruiser01",
    frontCamera = CameraTemplate(
        position = Vector3(0.0, 5.0, -4.7)
    ),
    physics = PhysicsTemplate()
)
