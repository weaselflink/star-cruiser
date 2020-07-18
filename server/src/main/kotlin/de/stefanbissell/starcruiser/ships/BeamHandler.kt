package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamMessage
import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType

class BeamHandler(
    private val beamWeapon: BeamWeapon
) {

    private var status: BeamStatus =
        BeamStatus.Idle
    private var targetSystemType = PoweredSystemType.random()

    fun update(
        time: GameTime,
        boostLevel: Double,
        shipProvider: (ObjectId) -> Ship?,
        lockHandler: LockHandler?,
        ship: Ship
    ) {
        val lockedTargetInRange = isLockedTargetInRange(shipProvider, lockHandler, ship)
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
                            targetSystemType =
                                PoweredSystemType.random()
                            BeamStatus.Firing()
                        } else {
                            BeamStatus.Idle
                        }
                    } else it
                }
            }
            is BeamStatus.Firing -> {
                status = current.update(time.delta * beamWeapon.firingSpeed).let {
                    if (it.progress >= 1.0) BeamStatus.Recharging() else it
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

    private fun getLockedTarget(shipProvider: (ObjectId) -> Ship?, lockHandler: LockHandler?) =
        getLockedTargetId(lockHandler)?.let { shipProvider(it) }

    private fun isLockedTargetInRange(shipProvider: (ObjectId) -> Ship?, lockHandler: LockHandler?, ship: Ship) =
        getLockedTarget(shipProvider, lockHandler)
            ?.toScopeContactMessage(ship)
            ?.relativePosition
            ?.rotate(-ship.rotation)
            ?.let { beamWeapon.isInRange(it) }
            ?: false
}
