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
        game.adjustInput(0, wrapDouble(game.solutions[0] + 0.2))
        game.adjustInput(1, wrapDouble(game.solutions[1] + 0.2))

        expectThat(game.isSolved).isFalse()
    }

    @Test
    fun `can make solved`() {
        val game = FrequencyGame.createUnsolved(2)
        game.adjustInput(0, game.solutions[0] + 0.05)
        game.adjustInput(1, game.solutions[1] + 0.05)

        expectThat(game.isSolved).isTrue()
    }

    @Test
    fun `ignores wrong dimension index`() {
        val game = FrequencyGame.createUnsolved(2)
        val inputBeforeChanges = game.inputs.toList()
        game.adjustInput(-1, Random.nextDouble())
        game.adjustInput(2, Random.nextDouble())

        expectThat(game.inputs).containsExactly(inputBeforeChanges)
    }

    @Test
    fun `clamps input values`() {
        val game = FrequencyGame.createUnsolved(2)
        game.adjustInput(0, -0.1)
        game.adjustInput(1, 1.1)

        expectThat(game.inputs).containsExactly(0.0, 1.0)
    }

    private fun wrapDouble(value: Double) =
        if (value >= 1.0) value - 1.0 else value

    private val FrequencyGame.isSolved
        get() = noise < 0.1
}
