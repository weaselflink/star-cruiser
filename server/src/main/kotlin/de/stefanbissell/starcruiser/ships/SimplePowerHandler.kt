package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.clamp

class SimplePowerHandler(
    private val shipTemplate: ShipTemplate
) {

    val poweredSystems = PoweredSystemType.values()
        .associate { it to PoweredSystem(it) }
        .toMutableMap()
    private var repairHandler: TimedRepairHandler? = null
    val repairing
        get() = repairHandler != null
    val repairingType
        get() = repairHandler?.type
    val systemsDamage
        get() = poweredSystems.entries
            .associate { it.key to 1.0 - it.value.damage }

    fun update(time: GameTime) {
        repairHandler?.apply {
            update(time)
            if (isComplete) {
                getPoweredSystem(type).repair(shipTemplate.repairAmount)
                repairHandler = null
            }
        }
    }

    fun takeDamage(type: PoweredSystemType, amount: Double) {
        getPoweredSystem(type).takeDamage(amount)
    }

    fun startRepair(type: PoweredSystemType) {
        getPoweredSystem(type).also {
            if (it.canRepair() && repairHandler?.type != type) {
                repairHandler = TimedRepairHandler(shipTemplate, type)
            }
        }
    }

    private fun getPoweredSystem(type: PoweredSystemType) =
        poweredSystems[type] ?: PoweredSystem(type)

    inner class PoweredSystem(
        val type: PoweredSystemType
    ) {

        var damage: Double = 0.0
            set(value) {
                field = value.clamp(0.0, 1.0)
            }

        fun takeDamage(amount: Double) {
            damage += amount / shipTemplate.poweredSystemDamageCapacity
        }

        fun canRepair() = damage > 0.0

        fun repair(amount: Double) {
            damage -= amount
        }
    }
}

private class TimedRepairHandler(
    private val shipTemplate: ShipTemplate,
    val type: PoweredSystemType
) {

    private var progress: Double = 0.0
    val isComplete
        get() = progress >= 1.0

    fun update(time: GameTime) {
        progress += shipTemplate.repairSpeed * time.delta
    }
}
