package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.GameState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class TriggerHandlerTest {

    private val scenario = EmptyScenario.create()

    private var currentTime = 0.0
    private var interval = 5.0
    private var condition = true
    private var actionPerformed = false
    private val triggerStates = mutableListOf<String>()

    private val gameState = mockk<GameState>().also {
        every { it.toView() } answers {
            GameStateView(
                scenario = scenario,
                currentTime = currentTime,
                ships = emptyList()
            )
        }
    }

    private var triggerHandler: TriggerHandler<*>? = null

    @Test
    fun `waits for interval to retrigger`() {
        evaluate()
        expectThat(actionPerformed).isTrue()

        evaluate()
        expectThat(actionPerformed).isFalse()
    }

    @Test
    fun `does not trigger on failed condition`() {
        condition = false

        evaluate()
        expectThat(actionPerformed).isFalse()
    }

    @Test
    fun `retriggers action`() {
        evaluate()
        expectThat(actionPerformed).isTrue()

        elapseInterval()

        evaluate()
        expectThat(actionPerformed).isTrue()
    }

    @Test
    fun `stores modified state from action`() {
        evaluate()

        elapseInterval()

        evaluate()

        elapseInterval()

        evaluate()

        expectThat(triggerStates)
            .containsExactly("", "X", "XX")
    }

    private fun elapseInterval() {
        currentTime += 6.0
    }

    private fun evaluate() {
        actionPerformed = false
        triggerHandler = triggerHandler ?: TriggerHandler(
            Trigger(
                interval = interval,
                condition = { condition },
                action = { triggerState ->
                    actionPerformed = true
                    triggerStates += triggerState
                    triggerState + "X"
                },
                initialState = { "" }
            )
        )
        triggerHandler?.evaluate(
            GameStateMutator(
                state = gameState,
                scenario = scenario
            )
        )
    }
}
