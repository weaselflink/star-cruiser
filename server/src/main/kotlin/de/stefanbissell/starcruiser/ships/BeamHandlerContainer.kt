package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamsMessage
import de.stefanbissell.starcruiser.clamp
import kotlin.random.Random

class BeamHandlerContainer(
    beamWeapons: List<BeamWeapon>,
    private val ship: Ship
) {

    var modulation: Int = Random.nextInt(7)
        set(value) {
            field = value.clamp(0, 7)
        }
    val beamHandlers = beamWeapons.map {
        BeamHandler(it, ship, this)
    }

    fun toMessage(lockHandler: LockHandler?) =
        BeamsMessage(
            modulation = modulation,
            beams = beamHandlers.map { it.toMessage(lockHandler) }
        )
}
