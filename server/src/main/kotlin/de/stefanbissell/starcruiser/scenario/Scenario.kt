package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.Vector2
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

abstract class Scenario {

    abstract val definition: ScenarioDefinition

    fun create(): ScenarioInstance {
        return ScenarioInstance(
            asteroids = definition.asteroidFields.flatMap { it.createAsteroids() }
        )
    }
}

fun scenario(block: ScenarioDefinition.() -> Unit): ScenarioDefinition {
    return ScenarioDefinition().apply(block)
}

class ScenarioDefinition {

    val asteroidFields = mutableListOf<AsteroidFieldDefinition>()

    fun asteroidField(block: AsteroidFieldDefinition.() -> Unit) {
        asteroidFields += AsteroidFieldDefinition().apply(block)
    }
}

class AsteroidFieldDefinition {

    lateinit var area: Area
    var density = 100.0

    private val asteroidCount: Int
        get() {
            val box = area.boundingBox
            return (box.width * box.height / 10_000.0 * density).roundToInt()
        }

    fun area(vararg boundary: Vector2) {
        area = Polygon.of(*boundary)
    }

    fun createAsteroids(): List<Asteroid> =
        (1..asteroidCount).map {
            Asteroid(
                position = randomPointInside(),
                rotation = Random.nextDouble(PI * 2.0),
                radius = Random.nextDouble(8.0, 32.0)
            )
        }

    private fun randomPointInside(): Vector2 {
        val box = area.boundingBox
        var position = box.randomPointInside()
        while (!area.isInside(position)) {
            position = box.randomPointInside()
        }
        return position
    }
}

data class ScenarioInstance(
    val asteroids: List<Asteroid>
)
