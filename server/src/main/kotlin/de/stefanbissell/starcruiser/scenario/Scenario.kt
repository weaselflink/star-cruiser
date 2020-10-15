package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.shapes.Circle
import de.stefanbissell.starcruiser.shapes.Shape
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

abstract class Scenario {

    abstract val definition: ScenarioDefinition

    fun create(): ScenarioInstance {
        val factions = definition.factions.create()
        return ScenarioInstance(
            playerSpawnArea = definition.playerSpawnArea,
            factions = factions,
            asteroids = definition.asteroidFields.flatMap {
                it.create()
            },
            nonPlayerShips = definition.nonPlayerShips.map {
                it.create(factions)
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

    var playerSpawnArea: Shape = Circle(Vector2(), 100)
    lateinit var factions: FactionsDefinition
    val asteroidFields = mutableListOf<AsteroidFieldDefinition>()
    val nonPlayerShips = mutableListOf<NonPlayerShipDefinition>()

    fun factions(block: FactionsDefinition.() -> Unit) {
        factions = FactionsDefinition().apply(block)
    }

    fun nonPlayerShip(block: NonPlayerShipDefinition.() -> Unit) {
        nonPlayerShips += NonPlayerShipDefinition().apply(block)
    }

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

    fun create(): List<Asteroid> =
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

class NonPlayerShipDefinition {

    lateinit var faction: String
    lateinit var spawnArea: Shape

    fun create(factions: List<Faction>): NonPlayerShip =
        NonPlayerShip(
            position = spawnArea.randomPointInside(),
            rotation = Random.nextDouble(PI * 2.0),
            faction = factions.first { it.name == faction }
        )
}

class FactionsDefinition {

    val factions = mutableListOf<FactionDefinition>()

    fun faction(block: FactionDefinition.() -> Unit) {
        factions += FactionDefinition().apply(block)
    }

    fun create() =
        factions.map {
            Faction(
                name = it.name,
                enemies = it.enemies,
                forPlayers = it.forPlayers
            )
        }
}

class FactionDefinition {

    lateinit var name: String
    var enemies = emptyList<String>()
    var forPlayers = false
}

data class ScenarioInstance(
    val playerSpawnArea: Shape,
    val factions: List<Faction>,
    val asteroids: List<Asteroid>,
    val nonPlayerShips: List<NonPlayerShip>,
    val mapAreas: List<MapArea>
)

data class Faction(
    val name: String,
    val enemies: List<String> = emptyList(),
    val forPlayers: Boolean = false
) {

    infix fun isHostileTo(other: Faction) =
        enemies.contains(other.name)
}

data class MapArea(
    val type: MapAreaType,
    val shape: Shape
)

enum class MapAreaType {
    AsteroidField
}
