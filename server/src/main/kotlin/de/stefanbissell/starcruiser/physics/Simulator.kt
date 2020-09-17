package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.ships.PlayerShip
import de.stefanbissell.starcruiser.ships.ShipTemplate
import de.stefanbissell.starcruiser.ships.cruiserTemplate
import de.stefanbissell.starcruiser.toRadians
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.abs

class Simulator(
    val shipTemplate: ShipTemplate
) {

    private val angularInterval = 0.2.toRadians()

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
        val gameTime = GameTime.atEpoch()
        val physicsEngine = PhysicsEngine()
        val ship = PlayerShip(template = shipTemplate, rotation = 0.0)

        init {
            physicsEngine.addShip(ship, false)
        }

        fun analyzeLinearAcceleration(): LinearAccelerationData {
            ship.throttle = 100
            val points = tickUntil { before, after ->
                abs(before.bodyParameters.speed.length() - after.bodyParameters.speed.length()) < 0.00001
            }

            val absoluteMax = points.last().bodyParameters.speed.length()
            val pointAt99 = points.first { it.bodyParameters.speed.length() > absoluteMax * 0.99 }
            return LinearAccelerationData(
                maxSpeed = pointAt99.bodyParameters.speed.length().twoDigits(),
                timeToMax = pointAt99.time.fiveDigits(),
                distanceToMax = pointAt99.bodyParameters.position.x.twoDigits()
            )
        }

        fun analyzeLinearDeceleration(maxSpeed: Double): LinearDecelerationData {
            physicsEngine.setShipSpeed(ship.id, Vector2(maxSpeed, 0))
            val points = tickUntil { _, after ->
                after.bodyParameters.speed.length() < 0.001
            }

            return LinearDecelerationData(
                timeToStop = points.last().time.fiveDigits(),
                distanceToStop = points.last().bodyParameters.position.x.twoDigits()
            )
        }

        fun analyzeAngularAcceleration(): AngularAccelerationData {
            ship.rudder = 100
            val points = tickUntil { before, after ->
                abs(before.bodyParameters.rotationSpeed - after.bodyParameters.rotationSpeed) < 0.00001
            }

            val absoluteMax = points.last().bodyParameters.rotationSpeed
            val indexAt99 = points.indexOfFirst { it.bodyParameters.rotationSpeed > absoluteMax * 0.99 }
            val pointAt99 = points[indexAt99]
            val relevantPoints = points.subList(0, indexAt99 + 1)
            return AngularAccelerationData(
                maxRotationSpeed = pointAt99.bodyParameters.rotationSpeed.fiveDigits(),
                timeToMax = pointAt99.time.fiveDigits(),
                rotationToMax = pointAt99.bodyParameters.rotation.fiveDigits(),
                profile = createAngularAccelerationProfile(relevantPoints)
            )
        }

        fun analyzeAngularDeceleration(maxRotationSpeed: Double): AngularDecelerationData {
            physicsEngine.setShipRotationSpeed(ship.id, maxRotationSpeed)
            val points = tickUntil { _, after ->
                after.bodyParameters.rotationSpeed < maxRotationSpeed * 0.01
            }

            return AngularDecelerationData(
                timeToStop = points.last().time.fiveDigits(),
                rotationToStop = points.last().bodyParameters.rotation.fiveDigits(),
                profile = createAngularDecelerationProfile(points)
            )
        }

        private fun createAngularAccelerationProfile(points: List<SimulationState>): List<AngularState> {
            val profile = mutableListOf<AngularState>()
            points.map {
                AngularState(
                    time = it.time.fiveDigits(),
                    rotation = it.bodyParameters.rotation.fiveDigits(),
                    rotationSpeed = it.bodyParameters.rotationSpeed.fiveDigits()
                )
            }.forEachIndexed { index, point ->
                if (index == 0 || index == points.size - 1) {
                    profile += point
                } else {
                    if (abs(profile.last().rotation - point.rotation) >= angularInterval) {
                        profile += point
                    }
                }
            }
            return profile
        }

        private fun createAngularDecelerationProfile(points: List<SimulationState>): List<AngularState> {
            val profile = mutableListOf<AngularState>()
            val rotationToStop = points.last().bodyParameters.rotation
            points.map {
                AngularState(
                    time = it.time.fiveDigits(),
                    rotation = (rotationToStop - it.bodyParameters.rotation).fiveDigits(),
                    rotationSpeed = it.bodyParameters.rotationSpeed.fiveDigits()
                )
            }.forEachIndexed { index, point ->
                if (index == 0 || index == points.size - 1) {
                    profile += point
                } else {
                    if (abs(profile.last().rotation - point.rotation) >= angularInterval) {
                        profile += point
                    }
                }
            }
            return profile
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
            ship.update(gameTime, physicsEngine)
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
    val rotationToMax: Double,
    val profile: List<AngularState>
)

data class AngularDecelerationData(
    val timeToStop: Double,
    val rotationToStop: Double,
    val profile: List<AngularState>
)

data class AngularState(
    val time: Double,
    val rotation: Double,
    val rotationSpeed: Double
)

data class PerformanceAnalysis(
    val linearAccelerationData: LinearAccelerationData,
    val linearDecelerationData: LinearDecelerationData,
    val angularAccelerationData: AngularAccelerationData,
    val angularDecelerationData: AngularDecelerationData
)

fun main() {
    val analysis = Simulator(
        cruiserTemplate.copy(
            throttleResponsiveness = 10_000.0
        )
    ).analyzeShip()
    println("time,rotation,speed")
    analysis.angularAccelerationData.profile.forEach {
        println("${it.time},${it.rotation},${it.rotationSpeed}")
    }
    analysis.angularDecelerationData.profile.forEach {
        println("${it.time},${it.rotation},${it.rotationSpeed}")
    }
}
