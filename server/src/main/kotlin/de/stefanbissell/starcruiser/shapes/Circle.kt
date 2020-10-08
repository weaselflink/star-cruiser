package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.MapAreaMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.randomAngle
import kotlin.math.sqrt
import kotlin.random.Random

class Circle(
    private val center: Vector2,
    private val radius: Double
) : Shape {

    constructor(
        center: Vector2,
        radius: Number
    ) : this(center, radius.toDouble())

    override val boundingBox: Box =
        Box(
            bottomLeft = center - Vector2(radius, radius),
            topRight = center + Vector2(radius, radius)
        )

    override fun isInside(point: Vector2): Boolean =
        (point - center).length() <= radius

    override fun randomPointInside(): Vector2 =
        Vector2(radius * sqrt(Random.nextDouble()))
            .rotate(randomAngle()) + center

    override fun toMessage(): MapAreaMessage =
        MapAreaMessage.Circle(
            center = center,
            radius = radius
        )
}
