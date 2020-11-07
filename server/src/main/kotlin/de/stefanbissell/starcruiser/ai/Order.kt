package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.Vector2

sealed class Order {

    object SeekAndDestroy : Order()

    data class Patrol(
        val path: List<Vector2>
    ) : Order()
}
