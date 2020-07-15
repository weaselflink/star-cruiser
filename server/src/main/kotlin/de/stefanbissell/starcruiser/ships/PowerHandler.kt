package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.oneDigit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class PowerHandler(
    private val shipTemplate: ShipTemplate
) {

    private var capacitors = shipTemplate.maxCapacitors
    private val poweredSystems = PoweredSystemType.values()
        .associate { it to PoweredSystem() }
        .toMutableMap()

    fun update(time: GameTime) {
        updateCapacitors(time)
        updateHeatLevels(time)
    }

    fun getPoweredSystem(type: PoweredSystemType) =
        poweredSystems[type] ?: PoweredSystem()

    private fun updateCapacitors(time: GameTime) {
        val capacitorPlus = shipTemplate.reactorOutput * getPoweredSystem(PoweredSystemType.Reactor).boostLevel
        val capacitorMinus = PoweredSystemType.values()
            .filter { it != PoweredSystemType.Reactor }
            .map {
                getPoweredSystem(it)
            }
            .map { it.level }
            .sum()
            .toDouble()

        capacitors += time.delta * (capacitorPlus - capacitorMinus) / 60
        capacitors = max(0.0, min(shipTemplate.maxCapacitors, capacitors))
    }

    private fun updateHeatLevels(time: GameTime) {
        PoweredSystemType.values().map {
            getPoweredSystem(it)
        }.forEach {
            it.updateHeat(time)
        }
    }

    fun toMessage() =
        PowerMessage(
            capacitors = capacitors.oneDigit(),
            maxCapacitors = shipTemplate.maxCapacitors,
            settings = PoweredSystemType.values().associate {
                val poweredSystem = getPoweredSystem(it)
                it to PoweredSystemMessage(
                    level = poweredSystem.level,
                    heat = poweredSystem.heat.fiveDigits(),
                    coolant = poweredSystem.coolant.fiveDigits()
                )
            }
        )
}

typealias BoostLevel = () -> Double

class PoweredSystem {

    var level: Int = 100
        set(value) {
            field = ((value / 10.0).roundToInt() * 10).clamp(0, 200)
        }
    var heat: Double = 0.0
        set(value) {
            field = value.clamp(0.0, 1.0)
        }
    var coolant: Double = 0.0
        set(value) {
            field = value.clamp(0.0, 1.0)
        }

    val boostLevel: Double
        get() = level / 100.0

    fun updateHeat(time: GameTime) {
        val heatChangePerSecond = 1.7.pow(boostLevel - 1.0) - (1.01 + coolant)
        heat += heatChangePerSecond * time.delta
    }
}
