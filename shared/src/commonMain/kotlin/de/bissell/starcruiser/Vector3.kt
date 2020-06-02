package de.bissell.starcruiser

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
}