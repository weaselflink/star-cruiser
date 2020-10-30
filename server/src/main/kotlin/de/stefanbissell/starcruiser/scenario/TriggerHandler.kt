package de.stefanbissell.starcruiser.scenario

class TriggerHandler(
    val trigger: Trigger
) {

    private var fired = false
    private var lastEvaluation = -1_000_000.0

    fun evaluate(gameStateMutator: GameStateMutator) {
        if (shouldFire(gameStateMutator.view)) {
            fire(gameStateMutator)
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

    private fun fire(gameStateMutator: GameStateMutator) {
        fired = true
        lastEvaluation = gameStateMutator.view.currentTime
        trigger.action(gameStateMutator)
    }
}
