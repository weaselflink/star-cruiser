package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.ships.Ship

data class GameStateView(
    val currentTime: Double,
    val scenario: ScenarioInstance,
    val ships: List<Ship>
) {

    fun factionByName(name: String) =
        scenario.factions.first { it.name == name }
}
