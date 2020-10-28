package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.GameState

class TriggerHandler(
    val trigger: Trigger
) {

    private var fired = false
    private var lastEvaluation = -1_000_000.0

    fun evaluate(gameState: GameState) {
        if (shouldFire(gameState.toView())) {
            fire(gameState)
        }
    }

    private fun shouldFire(gameStateView: GameStateView): Boolean {
        if (!trigger.repeat && fired) {
            return false
        }
        if (gameStateView.currentTime - lastEvaluation < trigger.interval) {
            return false
        }

        return trigger.condition(gameStateView)
    }

    private fun fire(gameState: GameState) {
        fired = true
        val view = gameState.toView()
        lastEvaluation = view.currentTime
        trigger.action(gameState, view.scenario)
    }
}
