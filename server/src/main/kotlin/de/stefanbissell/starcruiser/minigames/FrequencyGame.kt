package de.stefanbissell.starcruiser.minigames

import de.stefanbissell.starcruiser.clamp
import kotlin.math.abs
import kotlin.random.Random

class FrequencyGame(
    val dimensions: Int
) {

    val solution: List<Double> = (0 until dimensions).map { Random.nextDouble() }
    val input: MutableList<Double> = solution.toMutableList()
    val noise
        get() = solution
            .mapIndexed { index, value -> abs(value - input[index]) }
            .sum() / dimensions

    fun adjustInput(dimension: Int, value: Double) {
        if (dimension >= 0 && dimension < input.size) {
            input[dimension] = value.clamp(0.0, 1.0)
        }
    }

    fun randomize() {
        while (noise < 0.2) {
            (0 until input.size).forEach {
                input[it] = Random.nextDouble()
            }
        }
    }

    companion object {
        fun createSolved(dimensions: Int) =
            FrequencyGame(dimensions)

        fun createUnsolved(dimensions: Int) =
            createSolved(dimensions).apply {
                randomize()
            }
    }
}
