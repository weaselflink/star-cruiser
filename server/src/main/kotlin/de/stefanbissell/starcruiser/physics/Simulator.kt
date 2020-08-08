package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipTemplate
import de.stefanbissell.starcruiser.ships.cruiserTemplate
import de.stefanbissell.starcruiser.toDegrees
import de.stefanbissell.starcruiser.twoDigits
import java.time.Instant
import kotlin.math.abs

class Simulator(
    val shipTemplate: ShipTemplate
) {

    fun analyzeShip() =
        PerformanceAnalysis(
            linearAccelerationData = analyzeLinearAcceleration(),
            angularAccelerationData = analyzeAngularAcceleration()
        )

    private fun analyzeLinearAcceleration() = SimulationRun().analyzeLinearAcceleration()

    private fun analyzeAngularAcceleration() = SimulationRun().analyzeAngularAcceleration()

    private inner class SimulationRun {
        val gameTime = GameTime(Instant.EPOCH)
        val physicsEngine = PhysicsEngine()
        val ship = Ship(template = shipTemplate)

        init {
            physicsEngine.addShip(ship)
        }

        fun analyzeLinearAcceleration(): LinearAccelerationData {
            ship.changeThrottle(100)
            val points = tickUntil { before, after ->
                abs(before.bodyParameters.speed.length() - after.bodyParameters.speed.length()) < 0.00001
            }

            val absoluteMax = points.last().bodyParameters.speed.length()
            val pointAt99 = points.first { it.bodyParameters.speed.length() > absoluteMax * 0.99 }
            return LinearAccelerationData(
                maxSpeed = pointAt99.bodyParameters.speed.length().twoDigits(),
                timeToMax = pointAt99.time.twoDigits(),
                distanceToMax = pointAt99.bodyParameters.position.y.twoDigits()
            )
        }

        fun analyzeAngularAcceleration(): AngularAccelerationData {
            ship.changeRudder(100)
            val points = tickUntil { before, after ->
                abs(before.bodyParameters.rotationSpeed - after.bodyParameters.rotationSpeed) < 0.00001
            }

            val absoluteMax = points.last().bodyParameters.rotationSpeed
            val pointAt99 = points.first { it.bodyParameters.rotationSpeed > absoluteMax * 0.99 }
            return AngularAccelerationData(
                maxRotationSpeed = pointAt99.bodyParameters.rotationSpeed.toDegrees().twoDigits(),
                timeToMax = pointAt99.time.twoDigits(),
                rotationToMax = pointAt99.bodyParameters.rotation.toDegrees().twoDigits()
            )
        }

        private fun tickUntil(condition: (SimulationState, SimulationState) -> Boolean): List<SimulationState> {
            val points = mutableListOf<SimulationState>()
            var (before, after) = tick()
            points += before
            while (!condition(before, after)) {
                tick().also {
                    before = it.first
                    after = it.second
                }
                points += after
            }
            return points
        }

        private fun tick(): Pair<SimulationState, SimulationState> {
            val before = SimulationState(physicsEngine.getBodyParameters(ship.id)!!, gameTime.current)
            gameTime.update(0.01)
            ship.update(gameTime, physicsEngine) { null }
            physicsEngine.step(gameTime.delta)
            val after = SimulationState(physicsEngine.getBodyParameters(ship.id)!!, gameTime.current)
            return before to after
        }
    }
}

data class SimulationState(
    val bodyParameters: BodyParameters,
    val time: Double
)

data class LinearAccelerationData(
    val maxSpeed: Double,
    val timeToMax: Double,
    val distanceToMax: Double
)

data class AngularAccelerationData(
    val maxRotationSpeed: Double,
    val timeToMax: Double,
    val rotationToMax: Double
)

data class PerformanceAnalysis(
    val linearAccelerationData: LinearAccelerationData,
    val angularAccelerationData: AngularAccelerationData
)

fun main() {
    println(
        Simulator(cruiserTemplate).analyzeShip()
    )
}
