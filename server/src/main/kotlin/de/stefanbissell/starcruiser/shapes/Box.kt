package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.MapAreaMessage
import de.stefanbissell.starcruiser.Vector2
import kotlin.math.abs
import kotlin.random.Random

data class Box(
    val bottomLeft: Vector2,
    val topRight: Vector2
) : Shape {

    val width = abs(topRight.x - bottomLeft.x)
    val height = abs(topRight.y - bottomLeft.y)
    val area = width * height

    override val boundingBox: Box = this

    override fun isInside(point: Vector2): Boolean =
        point.x >= bottomLeft.x && point.y >= bottomLeft.y &&
            point.x <= topRight.x && point.y <= topRight.y

    override fun toMessage(): MapAreaMessage =
        MapAreaMessage.Polygon(
            listOf(
                bottomLeft,
                bottomLeft + Vector2(0, height),
                topRight,
                bottomLeft + Vector2(width, 0)
            )
        )

    override fun randomPointInside() =
        bottomLeft + Vector2(Random.nextDouble(width), Random.nextDouble(height))
}
