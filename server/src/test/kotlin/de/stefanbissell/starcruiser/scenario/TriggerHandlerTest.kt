package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.GameState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class TriggerHandlerTest {

    private var currentTime = 0.0
    private var interval = 5.0
    private var repeat = true
    private var condition = true
    private var actionPerformed = false

    private val gameStateView = mockk<GameStateView>(relaxed = true).also {
        every { it.currentTime } answers { currentTime }
    }
    private val gameState = mockk<GameState>().also {
        every { it.toView() } returns gameStateView
    }

    private var triggerHandler: TriggerHandler? = null

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
    fun `retriggers if repeat is enabled`() {
        evaluate()
        expectThat(actionPerformed).isTrue()

        elapseInterval()

        evaluate()
        expectThat(actionPerformed).isTrue()
    }

    @Test
    fun `does not retriggers if repeat is disabled`() {
        repeat = false

        evaluate()
        expectThat(actionPerformed).isTrue()

        elapseInterval()

        evaluate()
        expectThat(actionPerformed).isFalse()
    }

    private fun elapseInterval() {
        currentTime += 6.0
    }

    private fun evaluate() {
        actionPerformed = false
        triggerHandler = triggerHandler ?: TriggerHandler(
            Trigger(interval, repeat, { condition }, { actionPerformed = true })
        )
        triggerHandler?.evaluate(gameState)
    }
}
