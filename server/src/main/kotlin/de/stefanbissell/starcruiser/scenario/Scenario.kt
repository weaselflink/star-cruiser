package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.shapes.Polygon
import de.stefanbissell.starcruiser.shapes.Shape
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

abstract class Scenario {

    abstract val definition: ScenarioDefinition

    fun create(): ScenarioInstance {
        return ScenarioInstance(
            asteroids = definition.asteroidFields.flatMap {
                it.createAsteroids()
            },
            mapAreas = definition.asteroidFields.map {
                it.toMapArea()
            }
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

    lateinit var shape: Shape
    var density: Number = 1

    private val asteroidCount: Int
        get() {
            val box = shape.boundingBox
            return (box.area / 10_000 * density.toDouble()).roundToInt()
        }

    fun area(vararg boundary: Vector2) {
        shape = Polygon.of(*boundary)
    }

    fun createAsteroids(): List<Asteroid> =
        (1..asteroidCount).map {
            Asteroid(
                position = randomPointInside(),
                rotation = Random.nextDouble(PI * 2.0),
                radius = Random.nextDouble(8.0, 32.0)
            )
        }

    fun toMapArea() =
        MapArea(
            type = MapAreaType.AsteroidField,
            shape = shape
        )

    private fun randomPointInside(): Vector2 {
        val box = shape.boundingBox
        var position = box.randomPointInside()
        while (!shape.isInside(position)) {
            position = box.randomPointInside()
        }
        return position
    }
}

data class ScenarioInstance(
    val asteroids: List<Asteroid>,
    val mapAreas: List<MapArea>
)

data class MapArea(
    val type: MapAreaType,
    val shape: Shape
)

enum class MapAreaType {
    AsteroidField
}
