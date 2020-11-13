package de.stefanbissell.starcruiser.ships

class TubeHandlerContainer(
    launchTubes: List<LaunchTube>,
    val magazine: Magazine,
    val ship: Ship
) {

    val tubeHandlers = launchTubes.map {
        TubeHandler(it, ship)
    }
}
