package de.stefanbissell.starcruiser.ships

class TubeHandlerContainer(
    launchTubes: List<LaunchTube>,
    val magazine: Magazine,
    val ship: Ship
) {

    val tubeHandlers = launchTubes.map {
        TubeHandler(it, ship)
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
