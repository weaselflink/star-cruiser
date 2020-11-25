package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.BeamsMessage
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.ships.BeamWeapon
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.Ship
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
    val damageEvents
        get() = beamHandlers.mapNotNull { it.damageEvent }

    fun update(
        time: GameTime,
        boostLevel: Double = 1.0,
        contactList: ContactList,
        lockHandler: LockHandler?,
        physicsEngine: PhysicsEngine
    ) {
        beamHandlers.forEach {
            it.update(
                time = time,
                boostLevel = boostLevel,
                contactList = contactList,
                lockHandler = lockHandler,
                physicsEngine = physicsEngine
            )
        }
    }

    fun toMessage(lockHandler: LockHandler?) =
        BeamsMessage(
            modulation = modulation,
            beams = beamHandlers.map { it.toMessage(lockHandler) }
        )
}
