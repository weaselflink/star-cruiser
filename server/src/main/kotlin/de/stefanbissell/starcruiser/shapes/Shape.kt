package de.stefanbissell.starcruiser.shapes

import de.stefanbissell.starcruiser.MapAreaMessage
import de.stefanbissell.starcruiser.Vector2

interface Shape {

    val boundingBox: Box
    fun isInside(point: Vector2): Boolean
    fun randomPointInside(): Vector2
    fun toMessage(): MapAreaMessage
}
