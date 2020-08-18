package de.stefanbissell.starcruiser.minigames

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class FrequencyGameTest {

    @Test
    fun `can create solved`() {
        repeat(10) {
            expectThat(
                FrequencyGame.createSolved(2).isSolved
            ).isTrue()
        }
    }

    @Test
    fun `can create unsolved`() {
        repeat(10) {
            expectThat(
                FrequencyGame.createUnsolved(2).isSolved
            ).isFalse()
        }
    }

    @Test
    fun `can make unsolved`() {
        val game = FrequencyGame.createSolved(2)
        game.adjustInput(0, wrapDouble(game.solution[0] + 0.2))
        game.adjustInput(1, wrapDouble(game.solution[1] + 0.2))

        expectThat(game.isSolved).isFalse()
    }

    @Test
    fun `can make solved`() {
        val game = FrequencyGame.createUnsolved(2)
        game.adjustInput(0, wrapDouble(game.solution[0] + 0.05))
        game.adjustInput(1, wrapDouble(game.solution[1] + 0.05))

        expectThat(game.isSolved).isTrue()
    }

    private fun wrapDouble(value: Double) =
        if (value >= 1.0) value - 1.0 else value
}
