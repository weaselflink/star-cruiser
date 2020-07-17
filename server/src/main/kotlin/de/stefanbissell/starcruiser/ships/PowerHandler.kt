package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.oneDigit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class PowerHandler(
    private val shipTemplate: ShipTemplate
) {

    private var capacitors = shipTemplate.maxCapacitors
    private val poweredSystems = PoweredSystemType.values()
        .associate { it to PoweredSystem() }
        .toMutableMap()

    fun update(time: GameTime) {
        updateCapacitors(time)
        updateHeatLevels(time)
    }

    fun getBoostLevel(type: PoweredSystemType) = getPoweredSystem(type).boostLevel

    fun startRepair(type: PoweredSystemType) {
        poweredSystems
            .filter {
                it.key != type
            }
            .forEach {
                it.value.stopRepairing()
            }

        getPoweredSystem(type).startRepairing()
    }

    fun setLevel(type: PoweredSystemType, value: Int) {
        getPoweredSystem(type).level = value
    }

    fun setCoolant(type: PoweredSystemType, value: Double) {
        getPoweredSystem(type).coolant = value

        val totalCoolant = poweredSystems.values
            .map { it.coolant }
            .sum()

        if (totalCoolant > shipTemplate.maxCoolant) {
            val ratio = shipTemplate.maxCoolant / totalCoolant
            poweredSystems
                .filter { it.key != type }
                .forEach {
                    it.value.coolant *= ratio
                }
        }
    }

    fun toMessage() =
        PowerMessage(
            capacitors = capacitors.oneDigit(),
            maxCapacitors = shipTemplate.maxCapacitors,
            settings = poweredSystems.mapValues { it.value.toMessage() }
        )

    private fun getPoweredSystem(type: PoweredSystemType) =
        poweredSystems[type] ?: PoweredSystem()

    private fun updateCapacitors(time: GameTime) {
        val capacitorPlus = shipTemplate.reactorOutput * getPoweredSystem(PoweredSystemType.Reactor).boostLevel
        val capacitorMinus = poweredSystems
            .filter { it.key != PoweredSystemType.Reactor }
            .map { it.value.level }
            .sum()
            .toDouble()

        capacitors += time.delta * (capacitorPlus - capacitorMinus) / 60
        capacitors = max(0.0, min(shipTemplate.maxCapacitors, capacitors))
    }

    private fun updateHeatLevels(time: GameTime) {
        poweredSystems
            .forEach {
                it.value.update(time)
            }
    }

    inner class PoweredSystem {

        private var repairProgress: Double? = null
        private var damage: Double = 0.0
            set(value) {
                field = value.clamp(0.0, 1.0)
            }
        var level: Int = 100
            set(value) {
                field = ((value / 10.0).roundToInt() * 10).clamp(0, 200)
            }
        private val ratio
            get() = level * 0.01
        private var heat: Double = 0.0
            set(value) {
                field = value.clamp(0.0, 1.0)
            }
        var coolant: Double = 0.0
            set(value) {
                field = value.clamp(0.0, 1.0)
            }

        val boostLevel: Double
            get() = ratio * (1.0 - damage)

        fun update(time: GameTime) {
            updateHeat(time)
            updateRepairProgress(time)
        }

        private fun updateHeat(time: GameTime) {
            val heatChangePerSecond = 1.7.pow(ratio - 1.0) - (1.01 + coolant)
            val resultantHeat = heat + heatChangePerSecond * time.delta
            val overheat = resultantHeat - 1.0
            heat = resultantHeat

            if (overheat > 0.0) {
                damage += overheat * shipTemplate.heatDamage * time.delta
            }
        }

        private fun updateRepairProgress(time: GameTime) {
            var currentRepairProgress = repairProgress
            if (currentRepairProgress != null) {
                currentRepairProgress += shipTemplate.repairSpeed * time.delta

                if (currentRepairProgress > 1.0) {
                    repairProgress = null

                    damage -= shipTemplate.repairAmount
                } else {
                    repairProgress = currentRepairProgress
                }
            }
        }

        fun startRepairing() {
            if (repairProgress == null && damage > 0.0) {
                repairProgress = 0.0
            }
        }

        fun stopRepairing() {
            repairProgress = null
        }

        fun toMessage() =
            PoweredSystemMessage(
                repairProgress = repairProgress,
                damage = damage.fiveDigits(),
                level = level,
                heat = heat.fiveDigits(),
                coolant = coolant.fiveDigits()
            )
    }
}

typealias BoostLevel = () -> Double
