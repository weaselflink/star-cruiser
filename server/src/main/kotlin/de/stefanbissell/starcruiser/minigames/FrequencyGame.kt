package de.stefanbissell.starcruiser.minigames

import de.stefanbissell.starcruiser.clamp
import kotlin.math.abs
import kotlin.random.Random

class FrequencyGame(
    val dimensions: Int,
    val tolerance: Double
) {

    private val solution: List<Double> = (0 until dimensions).map { Random.nextDouble() }
    private val input: MutableList<Double> = solution.toMutableList()

    val isSolved: Boolean
        get() = solution
            .mapIndexed { index, value -> abs(value - input[index]) }
            .sum() < tolerance * dimensions

    fun adjustInput(dimension: Int, value: Double) {
        if (dimension >= 0 && dimension < input.size) {
            input[dimension] = value.clamp(0.0, 1.0)
        }
    }

    companion object {
        fun createUnsolved(
            dimensions: Int,
            tolerance: Double = 0.1
        ) = FrequencyGame(dimensions, tolerance)
    }
}
