package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime

class TubeHandlerContainer(
    launchTubes: List<LaunchTube>,
    val magazine: Magazine,
    val ship: Ship
) {

    val tubeHandlers = launchTubes.map {
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
        tubeHandlers.getOrNull(index)?.apply {
            startReload()
        }
    }
}
