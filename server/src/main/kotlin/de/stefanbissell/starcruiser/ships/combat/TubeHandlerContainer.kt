package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TubeDirectionMessage
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
    val torpedoes
        get() = tubeHandlers.filter {
            it.newTorpedo
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

    fun requestLaunch(index: Int) {
        tubeHandlers.getOrNull(index)?.apply {
            requestLaunch()
        }
    }

    fun requestReload(index: Int) {
        if (magazineRemaining > 0) {
            tubeHandlers.getOrNull(index)?.apply {
                if (requestReload()) {
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

    fun toDirectionMessage() =
        tubeHandlers.map {
            TubeDirectionMessage(
                position = it.tube.position,
                rotation = it.tube.direction.toRadians()
            )
        }
}
