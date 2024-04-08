package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.ships.carrierTemplate
import de.stefanbissell.starcruiser.ships.cruiserTemplate

object PerformanceAnalysisStore {

    private val store = listOf(
        carrierTemplate,
        cruiserTemplate
    ).map {
        Simulator(it).analyzeShip()
    }.associateBy {
        it.className
    }

    operator fun get(className: String) =
        store[className]
}

data class PerformanceAnalysis(
    val className: String,
    val linearAccelerationData: LinearAccelerationData,
    val linearDecelerationData: LinearDecelerationData,
    val angularAccelerationData: AngularAccelerationData,
    val angularDecelerationData: AngularDecelerationData
) {

    fun calculateTurn(angle: Double): Double {
        val accelerationProfile = angularAccelerationData.profile
        val decelerationProfile = angularDecelerationData.profile.asReversed()

        val result = accelerationProfile.lastOrNull { acc ->
            val corresponding = decelerationProfile.firstOrNull { dec ->
                dec.rotationSpeed >= acc.rotationSpeed
            }
            if (corresponding != null) {
                acc.rotation + corresponding.rotation <= angle
            } else {
                false
            }
        }?.rotation ?: 0.0

        val rotationToStop = decelerationProfile.last().rotation
        return if (result + rotationToStop < angle) {
            angle - rotationToStop
        } else {
            result
        }.fiveDigits()
    }
}

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
