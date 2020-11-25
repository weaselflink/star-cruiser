package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.BeamMessage
import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import de.stefanbissell.starcruiser.ships.BeamWeapon
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.Ship

class BeamHandler(
    private val beamWeapon: BeamWeapon,
    private val ship: Ship,
    private val parent: BeamHandlerContainer
) {

    private val position2d = beamWeapon.position.let { Vector2(it.x, it.y) }
    private val beamPosition
        get() = ship.position + (position2d.rotate(ship.rotation))

    private var status: BeamStatus = BeamStatus.Idle
    private var targetSystemType = PoweredSystemType.random()
    var damageEvent: DamageEvent? = null

    fun update(
        time: GameTime,
        boostLevel: Double = 1.0,
        contactList: ContactList,
        lockHandler: LockHandler?,
        physicsEngine: PhysicsEngine
    ) {
        damageEvent = null
        val lockedTargetInRange = isLockedTargetInRange(contactList, lockHandler, physicsEngine)
        when (val current = status) {
            is BeamStatus.Idle -> if (lockedTargetInRange) {
                targetSystemType = PoweredSystemType.random()
                status = BeamStatus.Firing()
            }
            is BeamStatus.Recharging -> {
                val currentProgress = time.delta * beamWeapon.rechargeSpeed * boostLevel
                status = current.update(currentProgress).let {
                    if (it.progress >= 1.0) {
                        if (lockedTargetInRange) {
                            targetSystemType = PoweredSystemType.random()
                            BeamStatus.Firing()
                        } else {
                            BeamStatus.Idle
                        }
                    } else it
                }
            }
            is BeamStatus.Firing -> {
                status = if (lockedTargetInRange) {
                    current.update(time.delta * beamWeapon.firingSpeed).let {
                        if (it.progress >= 1.0) BeamStatus.Recharging() else it
                    }
                } else {
                    BeamStatus.Recharging()
                }
            }
        }
        if (status is BeamStatus.Firing) {
            getLockedTarget(contactList, lockHandler)
                ?.asShip()
                ?.also {
                    it.takeDamage(targetSystemType, time.delta, parent.modulation)
                    damageEvent = DamageEvent.Beam(it.id, targetSystemType, time.delta, parent.modulation)
                }
        }
    }

    fun endUpdate(): DamageEvent? = damageEvent

    fun toMessage(lockHandler: LockHandler?) =
        BeamMessage(
            position = beamWeapon.position,
            minRange = beamWeapon.range.first.toDouble(),
            maxRange = beamWeapon.range.last.toDouble(),
            leftArc = beamWeapon.leftArc.toDouble(),
            rightArc = beamWeapon.rightArc.toDouble(),
            status = status,
            targetId = getLockedTargetId(lockHandler)
        )

    private fun getLockedTargetId(lockHandler: LockHandler?) =
        if (lockHandler?.isComplete == true) lockHandler.targetId else null

    private fun getLockedTarget(contactList: ContactList, lockHandler: LockHandler?) =
        getLockedTargetId(lockHandler)?.let { contactList[it] }

    private fun isLockedTargetInRange(
        contactList: ContactList,
        lockHandler: LockHandler?,
        physicsEngine: PhysicsEngine
    ) = getLockedTarget(contactList, lockHandler)?.let {
        inRange(it) && unobstructed(it, physicsEngine)
    } ?: false

    private fun inRange(contact: ContactList.Contact) =
        contact.relativePosition
            .rotate(-ship.rotation)
            .let { beamWeapon.isInRange(it) }

    private fun unobstructed(contact: ContactList.Contact, physicsEngine: PhysicsEngine): Boolean {
        val ignore = listOf(ship.id, contact.id)
        val obstructions = physicsEngine.findObstructions(beamPosition, contact.position, ignore)
        return obstructions.isEmpty()
    }
}
