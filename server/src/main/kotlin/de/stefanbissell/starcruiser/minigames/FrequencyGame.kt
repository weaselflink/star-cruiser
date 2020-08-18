package de.stefanbissell.starcruiser.minigames

import de.stefanbissell.starcruiser.clamp
import kotlin.math.abs
import kotlin.random.Random

class FrequencyGame(
    val dimensions: Int,
    val tolerance: Double
) {

    val solution: List<Double> = (0 until dimensions).map { Random.nextDouble() }
    val input: MutableList<Double> = solution.toMutableList()

    val isSolved: Boolean
        get() = solution
            .mapIndexed { index, value -> abs(value - input[index]) }
            .maxOrNull() ?: 0.0 < tolerance

    fun adjustInput(dimension: Int, value: Double) {
        if (dimension >= 0 && dimension < input.size) {
            input[dimension] = value.clamp(0.0, 1.0)
        }
    }

    fun randomize() {
        while (isSolved) {
            (0 until input.size).forEach {
                input[it] = Random.nextDouble()
            }
        }
    }

    companion object {
        fun createSolved(
            dimensions: Int,
            tolerance: Double = 0.1
        ) = FrequencyGame(dimensions, tolerance)

        fun createUnsolved(
            dimensions: Int,
            tolerance: Double = 0.1
        ) = createSolved(dimensions, tolerance).apply {
            randomize()
        }
    }
}
