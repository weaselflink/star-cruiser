package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Vector2

interface Shape {

    val boundingBox: Box

    fun isInside(point: Vector2): Boolean
}
