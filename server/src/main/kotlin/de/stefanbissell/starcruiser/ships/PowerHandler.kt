package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PowerHandler {

    private val powerSettings = PoweredSystem.values().associate { it to 100 }.toMutableMap()

    operator fun get(system: PoweredSystem): Int = powerSettings[system] ?: 100
    operator fun set(system: PoweredSystem, power: Int) {
        powerSettings[system] = max(0, min(200, (power / 10.0).roundToInt() * 10))
    }

    fun boostLevel(system: PoweredSystem) = this[system] / 100.0

    fun toMessage() =
        PowerMessage(
            settings = powerSettings.toMap()
        )
}

typealias BoostLevel = () -> Double
