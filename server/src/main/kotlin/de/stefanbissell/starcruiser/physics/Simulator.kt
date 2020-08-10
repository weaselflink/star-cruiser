package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.ShipTemplate
import de.stefanbissell.starcruiser.ships.cruiserTemplate
import de.stefanbissell.starcruiser.twoDigits
import java.time.Instant
import kotlin.math.abs

class Simulator(
    val shipTemplate: ShipTemplate
) {

    fun analyzeShip(): PerformanceAnalysis {
        val linearAccelerationData = analyzeLinearAcceleration()
        val angularAccelerationData = analyzeAngularAcceleration()

        return PerformanceAnalysis(
            linearAccelerationData = linearAccelerationData,
            linearDecelerationData = analyzeLinearDeceleration(linearAccelerationData.maxSpeed),
            angularAccelerationData = angularAccelerationData,
            angularDecelerationData = analyzeAngularDeceleration(angularAccelerationData.maxRotationSpeed)
        )
    }

    private fun analyzeLinearAcceleration() =
        SimulationRun().analyzeLinearAcceleration()

    private fun analyzeLinearDeceleration(maxSpeed: Double) =
        SimulationRun().analyzeLinearDeceleration(maxSpeed)

    private fun analyzeAngularAcceleration() =
        SimulationRun().analyzeAngularAcceleration()

    private fun analyzeAngularDeceleration(maxRotationSpeed: Double) =
        SimulationRun().analyzeAngularDeceleration(maxRotationSpeed)

    private inner class SimulationRun {
        val gameTime = GameTime(Instant.EPOCH)
        val physicsEngine = PhysicsEngine()
        val ship = Ship(template = shipTemplate, rotation = 0.0)

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
                distanceToMax = pointAt99.bodyParameters.position.x.twoDigits()
            )
        }

        fun analyzeLinearDeceleration(maxSpeed: Double): LinearDecelerationData {
            physicsEngine.setShipSpeed(ship.id, Vector2(maxSpeed, 0))
            val points = tickUntil { _, after ->
                after.bodyParameters.speed.length() < 0.001
            }

            return LinearDecelerationData(
                timeToStop = points.last().time.twoDigits(),
                distanceToStop = points.last().bodyParameters.position.x.twoDigits()
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
                maxRotationSpeed = pointAt99.bodyParameters.rotationSpeed.twoDigits(),
                timeToMax = pointAt99.time.twoDigits(),
                rotationToMax = pointAt99.bodyParameters.rotation.twoDigits()
            )
        }

        fun analyzeAngularDeceleration(maxRotationSpeed: Double): AngularDecelerationData {
            physicsEngine.setShipRotationSpeed(ship.id, maxRotationSpeed)
            val points = tickUntil { _, after ->
                after.bodyParameters.rotationSpeed < 0.00001
            }

            return AngularDecelerationData(
                timeToStop = points.last().time.twoDigits(),
                rotationToStop = points.last().bodyParameters.rotation.twoDigits()
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

data class LinearDecelerationData(
    val timeToStop: Double,
    val distanceToStop: Double
)

data class AngularAccelerationData(
    val maxRotationSpeed: Double,
    val timeToMax: Double,
    val rotationToMax: Double
)

data class AngularDecelerationData(
    val timeToStop: Double,
    val rotationToStop: Double
)

data class PerformanceAnalysis(
    val linearAccelerationData: LinearAccelerationData,
    val linearDecelerationData: LinearDecelerationData,
    val angularAccelerationData: AngularAccelerationData,
    val angularDecelerationData: AngularDecelerationData
)

fun main() {
    println(
        Simulator(
            cruiserTemplate.copy(
                throttleResponsiveness = 10_000.0
            )
        ).analyzeShip()
    )
}
