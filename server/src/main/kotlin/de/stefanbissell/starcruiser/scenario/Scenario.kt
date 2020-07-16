package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.Vector2

abstract class Scenario {

    abstract val definition: ScenarioDefinition

    fun create(): ScenarioInstance {
        return ScenarioInstance(
            asteroids = emptyList()
        )
    }
}

fun scenario(block: ScenarioDefinition.() -> Unit): ScenarioDefinition {
    return ScenarioDefinition().apply(block)
}

class ScenarioDefinition {

    private val asteroidFields = mutableListOf<AsteroidFieldDefinition>()

    fun asteroidField(block: AsteroidFieldDefinition.() -> Unit) {
        asteroidFields += AsteroidFieldDefinition().apply(block)
    }
}

class AsteroidFieldDefinition {

    lateinit var area: Area
    var density = 100.0

    fun area(vararg boundary: Vector2) {
        area = Polygon(boundary.toList())
    }
}

data class ScenarioInstance(
    val asteroids: List<Asteroid>
)
