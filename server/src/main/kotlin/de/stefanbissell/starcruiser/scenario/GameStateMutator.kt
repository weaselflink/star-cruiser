package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.GameState

data class GameStateMutator(
    val state: GameState,
    val view: GameStateView = state.toView(),
    val scenario: ScenarioInstance
)
