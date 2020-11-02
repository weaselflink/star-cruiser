package de.stefanbissell.starcruiser.scenario

class TriggerHandler<T>(
    val trigger: Trigger<T>
) {

    private var triggerState: T = trigger.initialState()
    private var fired = false
    private var lastEvaluation = -1_000_000.0

    fun evaluate(gameStateMutator: GameStateMutator) {
        if (shouldFire(gameStateMutator.view)) {
            fire(gameStateMutator)
        }
    }

    private fun shouldFire(gameStateView: GameStateView): Boolean {
        if (gameStateView.currentTime - lastEvaluation < trigger.interval) {
            return false
        }

        return trigger.condition(gameStateView, triggerState)
    }

    private fun fire(gameStateMutator: GameStateMutator) {
        fired = true
        lastEvaluation = gameStateMutator.view.currentTime
        triggerState = trigger.action(gameStateMutator, triggerState)
    }
}
