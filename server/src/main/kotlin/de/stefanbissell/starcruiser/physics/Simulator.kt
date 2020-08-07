package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipTemplate
import de.stefanbissell.starcruiser.ships.cruiserTemplate
import java.time.Instant
import kotlin.math.abs

class Simulator(
    val shipTemplate: ShipTemplate
) {

    fun analyzeShip() {
        SimulationRun().analyzeMaxSpeed()
        SimulationRun().analyzeTurnStart()
    }

    private inner class SimulationRun {
        val gameTime = GameTime(Instant.EPOCH)
        val physicsEngine = PhysicsEngine()
        val ship = Ship(template = shipTemplate)

        init {
            physicsEngine.addShip(ship)
        }

        fun analyzeMaxSpeed() {
            ship.changeThrottle(100)
            val final = tickUntil { before, after ->
                abs(before.speed.length() - after.speed.length()) < 0.0000001
            }

            println("maxSpeed:")
            println(final.speed.length())
            println(final.position.y)
            println(gameTime.current)
        }

        fun analyzeTurnStart() {
            ship.changeRudder(100)
            val final = tickUntil { before, after ->
                abs(before.rotationSpeed - after.rotationSpeed) < 0.0000001
            }

            println("turnStart:")
            println(final.rotationSpeed)
            println(final.rotation)
            println(gameTime.current)
        }

        private fun tickUntil(condition: (BodyParameters, BodyParameters) -> Boolean): BodyParameters {
            var (before, after) = tick()
            while (!condition(before, after)) {
                tick().also {
                    before = it.first
                    after = it.second
                }
            }
            return after
        }

        private fun tick(): Pair<BodyParameters, BodyParameters> {
            val before = physicsEngine.getBodyParameters(ship.id)!!
            gameTime.update(0.01)
            ship.update(gameTime, physicsEngine) { null }
            physicsEngine.step(gameTime.delta)
            val after = physicsEngine.getBodyParameters(ship.id)!!
            return before to after
        }
    }
}

fun main() {
    Simulator(cruiserTemplate).analyzeShip()
}
