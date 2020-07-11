package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PowerHandler {

    private val powerSettings = PoweredSystem.values().associate { it to 100 }.toMutableMap()

    operator fun get(poweredSystem: PoweredSystem) = powerSettings[poweredSystem]
    operator fun set(poweredSystem: PoweredSystem, power: Int) {
        powerSettings[poweredSystem] = max(0, min(200, (power / 10.0).roundToInt() * 10))
    }

    fun toMessage() =
        PowerMessage(
            settings = powerSettings
        )
}
