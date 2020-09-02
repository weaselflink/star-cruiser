package de.stefanbissell.starcruiser.minigames

import de.stefanbissell.starcruiser.clamp
import kotlin.math.abs
import kotlin.random.Random

class FrequencyGame(
    val dimensions: Int
) {

    val solutions: List<Double> = (0 until dimensions).map { Random.nextDouble() }
    val inputs: MutableList<Double> = solutions.toMutableList()
    val noise
        get() = (0 until dimensions)
            .map(::singleDimensionNoise)
            .sum() / dimensions

    fun adjustInput(dimension: Int, value: Double) {
        if (dimension >= 0 && dimension < inputs.size) {
            inputs[dimension] = value.clamp(0.0, 1.0)
        }
    }

    fun randomize() {
        while (noise < 0.2) {
            (0 until inputs.size).forEach {
                inputs[it] = Random.nextDouble()
            }
        }
    }

    private fun singleDimensionNoise(dimension: Int) =
        abs(solutions[dimension] - inputs[dimension])

    companion object {
        fun createSolved(dimensions: Int) =
            FrequencyGame(dimensions)

        fun createUnsolved(dimensions: Int) =
            createSolved(dimensions).apply {
                randomize()
            }
    }
}
