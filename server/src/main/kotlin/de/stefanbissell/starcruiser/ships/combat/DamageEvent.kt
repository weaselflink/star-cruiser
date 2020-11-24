package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType

sealed class DamageEvent {

    abstract val target: ObjectId

    data class Beam(
        override val target: ObjectId,
        val targetedSystem: PoweredSystemType,
        val amount: Double,
        val modulation: Int
    ) : DamageEvent()

    data class Torpedo(
        override val target: ObjectId,
        val amount: Double
    ) : DamageEvent()
}
