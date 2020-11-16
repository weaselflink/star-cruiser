package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TubesMessage

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
