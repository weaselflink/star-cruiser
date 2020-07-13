package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystem
import de.stefanbissell.starcruiser.oneDigit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PowerHandler(
    private val shipTemplate: ShipTemplate
) {

    private var capacitors = shipTemplate.maxCapacitors
    private val powerSettings = PoweredSystem.values().associate { it to 100 }.toMutableMap()

    fun update(time: GameTime) {
        val capacitorPlus = shipTemplate.reactorOutput * boostLevel(PoweredSystem.Reactor)
        val capacitorMinus = PoweredSystem.values()
            .filter { it != PoweredSystem.Reactor }
            .map { getPowerLevel(it) }
            .sum()
            .toDouble()

        capacitors += time.delta * (capacitorPlus - capacitorMinus) / 60
        capacitors = max(0.0, min(shipTemplate.maxCapacitors, capacitors))
    }

    fun getPowerLevel(system: PoweredSystem): Int = powerSettings[system] ?: 100

    fun setPowerLevel(system: PoweredSystem, power: Int) {
        powerSettings[system] = max(0, min(200, (power / 10.0).roundToInt() * 10))
    }

    fun boostLevel(system: PoweredSystem) = getPowerLevel(system) / 100.0

    fun toMessage() =
        PowerMessage(
            capacitors = capacitors.oneDigit(),
            settings = powerSettings.toMap()
        )
}

typealias BoostLevel = () -> Double
