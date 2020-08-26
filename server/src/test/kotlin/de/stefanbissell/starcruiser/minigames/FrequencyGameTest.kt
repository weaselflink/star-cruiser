package de.stefanbissell.starcruiser.minigames

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import kotlin.random.Random

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
        game.adjustInput(0, game.solution[0] + 0.05)
        game.adjustInput(1, game.solution[1] + 0.05)

        expectThat(game.isSolved).isTrue()
    }

    @Test
    fun `ignores wrong dimension index`() {
        val game = FrequencyGame.createUnsolved(2)
        val inputBeforeChanges = game.input.toList()
        game.adjustInput(-1, Random.nextDouble())
        game.adjustInput(2, Random.nextDouble())

        expectThat(game.input).containsExactly(inputBeforeChanges)
    }

    @Test
    fun `clamps input values`() {
        val game = FrequencyGame.createUnsolved(2)
        game.adjustInput(0, -0.1)
        game.adjustInput(1, 1.1)

        expectThat(game.input).containsExactly(0.0, 1.0)
    }

    private fun wrapDouble(value: Double) =
        if (value >= 1.0) value - 1.0 else value

    private val FrequencyGame.isSolved
        get() = isSolved(0.1)
}
