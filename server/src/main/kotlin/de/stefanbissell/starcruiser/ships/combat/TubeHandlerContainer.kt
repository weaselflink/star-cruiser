package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TubesMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ships.Magazine
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.Tube
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
            val direction = Vector2(1, 0).rotate(rotation)
            Torpedo(
                launcherId = ship.id,
                faction = ship.faction,
                position = ship.position +
                    it.tube.position2d.rotate(ship.rotation) +
                    direction * it.tube.torpedoTemplate.radius * 1.1,
                rotation = rotation,
                speed = direction * it.tube.velocity
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
