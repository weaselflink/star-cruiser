package de.bissell.starcruiser

import kotlin.math.sqrt
import kotlinx.serialization.Serializable

@Serializable
data class Vector3(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {

    constructor() : this(0.0, 0.0)

    constructor(
        x: Number = 0.0,
        y: Number = 0.0,
        z: Number = 0.0
    ) : this(x.toDouble(), y.toDouble(), z.toDouble())

    operator fun plus(other: Vector3): Vector3 =
        Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3): Vector3 =
        Vector3(x - other.x, y - other.y, z - other.z)

    fun length(): Double =
        sqrt(x * x + y * y + z * z)
}
