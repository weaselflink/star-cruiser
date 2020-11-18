package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TubesMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.toRadians

class TubeHandlerContainer(
    tubes: List<Tube>,
    val magazine: Magazine,
    val ship: Ship
) {

    var magazineRemaining = magazine.capacity
    val tubeHandlers = tubes.map {
        TubeHandler(it, ship)
    }

    fun update(
        time: GameTime,
        boostLevel: Double = 1.0
    ) {
        tubeHandlers.forEach {
            it.update(
                time = time,
                boostLevel = boostLevel
            )
        }
    }

    fun endUpdate(): List<Torpedo> =
        tubeHandlers.filter {
            it.endUpdate()
        }.map {
            val rotation = ship.rotation + it.tube.direction.toRadians()
            Torpedo(
                faction = ship.faction,
                position = ship.position + it.tube.position.toVector2().rotate(rotation),
                rotation = rotation,
                speed = Vector2(it.tube.velocity, 0).rotate(rotation)
            )
        }

    fun launch(index: Int) {
        tubeHandlers.getOrNull(index)?.apply {
            launch()
        }
    }

    fun startReload(index: Int) {
        if (magazineRemaining > 0) {
            tubeHandlers.getOrNull(index)?.apply {
                if (startReload()) {
                    magazineRemaining--
                }
            }
        }
    }

    fun toMessage() =
        TubesMessage(
            magazineMax = magazine.capacity,
            magazineRemaining = magazineRemaining,
            tubes = tubeHandlers.map {
                it.status
            }
        )
}
