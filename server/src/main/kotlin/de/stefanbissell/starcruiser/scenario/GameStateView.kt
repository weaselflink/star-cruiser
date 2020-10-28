package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.ships.Ship

data class GameStateView(
    val scenario: ScenarioInstance,
    val currentTime: Double,
    val ships: List<Ship>
)
