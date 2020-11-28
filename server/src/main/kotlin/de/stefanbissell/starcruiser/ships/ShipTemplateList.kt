package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.Vector3
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.shapes.Polygon
import kotlin.math.PI

val carrierTemplate = ShipTemplate()

val cruiserTemplate = ShipTemplate().copy(
    className = "Protector",
    model = "cruiser01",
    aheadThrustFactor = 1.0,
    reverseThrustFactor = 0.3,
    rudderFactor = 2.0,
    beams = listOf(
        BeamWeapon(
            position = Vector3(-5.26, 0.0, -15.33),
            rightArc = -10
        ),
        BeamWeapon(
            position = Vector3(5.26, 0.0, -15.33),
            leftArc = 10
        )
    ),
    tubes = listOf(
        Tube(
            designation = "Port",
            position = Vector3(-13.45, 0.0, -5),
            direction = 80
        ),
        Tube(
            designation = "Stbd",
            position = Vector3(13.45, 0.0, -5),
            direction = -80
        )
    ),
    shield = ShieldTemplate().copy(
        radius = 24.5
    ),
    frontCamera = CameraTemplate(
        position = Vector3(0.0, 5.0, -4.7)
    ),
    leftCamera = CameraTemplate(
        position = Vector3(-1.5, 5.0, -4.3),
        rotation = PI * 0.5
    ),
    rightCamera = CameraTemplate(
        position = Vector3(1.5, 5.0, -4.3),
        rotation = -PI * 0.5
    ),
    rearCamera = CameraTemplate(
        position = Vector3(0.0, 5.0, 4.7),
        rotation = PI
    ),
    physics = PhysicsTemplate(
        geometry = listOf(
            Geometry(
                Polygon.of(
                    p(-21.9, 7.9),
                    p(-6.2, 7.9),
                    p(-6.2, -7.9),
                    p(-21.9, -7.9)
                ),
                0.02
            ),
            Geometry(
                Polygon.of(
                    p(-11.6, 0),
                    p(-10.2, 5.7),
                    p(-7.9, 10.2),
                    p(-3.2, 12.9),
                    p(-3.2, -12.9),
                    p(-7.9, -10.2),
                    p(-10.2, -5.7)
                ),
                0.02
            ),
            Geometry(
                Polygon.of(
                    p(-3.2, 12.9),
                    p(2.65, 14.2),
                    p(8.45, 12.9),
                    p(13.2, 10.2),
                    p(13.2, -10.2),
                    p(8.45, -12.9),
                    p(2.65, -14.2),
                    p(-3.2, -12.9)
                ),
                0.02
            ),
            Geometry(
                Polygon.of(
                    p(13.2, 10.2),
                    p(15.5, 5.7),
                    p(15.7, 5.1),
                    p(15.0, 4.8),
                    p(14.3, 3.7),
                    p(13.8, 1.9),
                    p(13.6, 0.0)
                ),
                0.0
            ),
            Geometry(
                Polygon.of(
                    p(13.6, 0.0),
                    p(13.8, -1.9),
                    p(14.3, -3.7),
                    p(15.0, -4.8),
                    p(15.7, -5.1),
                    p(15.5, -5.7),
                    p(13.2, -10.2)
                ),
                0.0
            )
        )
    )
)
