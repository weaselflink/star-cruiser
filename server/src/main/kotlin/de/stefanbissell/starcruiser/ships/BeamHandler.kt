package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamMessage
import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.physics.PhysicsEngine

class BeamHandler(
    private val beamWeapon: BeamWeapon,
    private val ship: Ship
) {

    private var status: BeamStatus = BeamStatus.Idle
    private var targetSystemType = PoweredSystemType.random()
    private val position2d = beamWeapon.position.let { Vector2(it.x, it.y) }
    private val beamPosition
        get() = ship.position + (position2d.rotate(ship.rotation))

    fun update(
        time: GameTime,
        boostLevel: Double,
        contactList: ShipContactList,
        lockHandler: LockHandler?,
        physicsEngine: PhysicsEngine
    ) {
        val shipProvider = contactList.shipProvider

        val lockedTargetInRange = isLockedTargetInRange(shipProvider, lockHandler, physicsEngine)
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
            getLockedTarget(shipProvider, lockHandler)?.takeDamage(targetSystemType, time.delta)
        }
    }

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

    private fun getLockedTarget(shipProvider: ShipProvider, lockHandler: LockHandler?) =
        getLockedTargetId(lockHandler)?.let { shipProvider(it) }

    private fun isLockedTargetInRange(
        shipProvider: ShipProvider,
        lockHandler: LockHandler?,
        physicsEngine: PhysicsEngine
    ) = getLockedTarget(shipProvider, lockHandler)?.let {
        inRange(it) && unobstructed(it, physicsEngine)
    } ?: false

    private fun inRange(target: Ship) =
        (target.position - ship.position)
            .rotate(-ship.rotation)
            .let { beamWeapon.isInRange(it) }

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    private fun unobstructed(target: Ship, physicsEngine: PhysicsEngine): Boolean {
        val ignore = listOf(ship.id, target.id)
        val obstructions = physicsEngine.findObstructions(beamPosition, target.position, ignore)
        return obstructions.isEmpty()
    }
}
