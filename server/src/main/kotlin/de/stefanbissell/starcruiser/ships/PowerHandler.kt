package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.oneDigit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PowerHandler(
    private val shipTemplate: ShipTemplate
) {

    private var capacitors = shipTemplate.maxCapacitors
    private val powerSettings = PoweredSystemType.values().associate { it to 100 }.toMutableMap()
    private val coolantSettings = PoweredSystemType.values().associate { it to 0.0 }.toMutableMap()

    fun update(time: GameTime) {
        val capacitorPlus = shipTemplate.reactorOutput * boostLevel(PoweredSystemType.Reactor)
        val capacitorMinus = PoweredSystemType.values()
            .filter { it != PoweredSystemType.Reactor }
            .map { getPowerLevel(it) }
            .sum()
            .toDouble()

        capacitors += time.delta * (capacitorPlus - capacitorMinus) / 60
        capacitors = max(0.0, min(shipTemplate.maxCapacitors, capacitors))
    }

    fun getPowerLevel(systemType: PoweredSystemType): Int = powerSettings[systemType] ?: 100

    fun setPowerLevel(systemType: PoweredSystemType, power: Int) {
        powerSettings[systemType] = max(0, min(200, (power / 10.0).roundToInt() * 10))
    }

    private fun getCoolantLevel(systemType: PoweredSystemType): Double = coolantSettings[systemType] ?: 0.0

    fun setCoolantLevel(systemType: PoweredSystemType, coolant: Double) {
        coolantSettings[systemType] = max(0.0, min(1.0, coolant))
    }

    fun boostLevel(systemType: PoweredSystemType) = getPowerLevel(systemType) / 100.0

    fun toMessage() =
        PowerMessage(
            capacitors = capacitors.oneDigit(),
            maxCapacitors = shipTemplate.maxCapacitors,
            settings = PoweredSystemType.values().associate {
                it to PoweredSystemMessage(
                    level = getPowerLevel(it),
                    heat = 0.0.fiveDigits(),
                    coolant = getCoolantLevel(it).fiveDigits()
                )
            }
        )
}

typealias BoostLevel = () -> Double
