package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Vector2

data class Box(
    private val bottomLeft: Vector2,
    private val topRight: Vector2
) : Area {

    override val boundingBox: Box = this

    override fun isInside(point: Vector2): Boolean =
        point.x >= bottomLeft.x && point.y >= bottomLeft.y
            && point.x <= topRight.x && point.y <= topRight.y
}
