package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.MapAreaMessage
import de.stefanbissell.starcruiser.Vector2
import kotlin.math.PI
import kotlin.random.Random

data class Ring(
    private val center: Vector2,
    private val outer: Double,
    private val inner: Double
) : Shape {

    override val boundingBox: Box
        get() = Box(
            bottomLeft = center - Vector2(outer, outer),
            topRight = center + Vector2(outer, outer)
        )

    override fun isInside(point: Vector2): Boolean =
        (point - center).length() in inner..outer

    override fun randomPointInside() =
        Vector2(Random.nextDouble(inner, outer), 0)
            .rotate(Random.nextDouble(2 * PI)) + center

    override fun toMessage(): MapAreaMessage {
        throw UnsupportedOperationException()
    }
}
