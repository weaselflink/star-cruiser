package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.RepairProgressMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.oneDigit
import kotlin.math.pow
import kotlin.math.roundToInt

class PowerHandler(
    private val shipTemplate: ShipTemplate
) {

    private var boostLevelModifier = 1.0
    private var capacitors = shipTemplate.maxCapacitors
    private val maxCapacitors
        get() = shipTemplate.maxCapacitors
    private val poweredSystems = PoweredSystemType.values()
        .associate { it to PoweredSystem() }
        .toMutableMap()

    private var powerGenerated: Double = 0.0
    private var powerUsed: Double = 0.0

    fun update(time: GameTime) {
        generatePower(time)
        drainPower(time)
        finalizeCapacitorCharge()
        updateHeatLevels(time)
    }

    fun takeDamage(type: PoweredSystemType, amount: Double) {
        getPoweredSystem(type).takeDamage(amount)
    }

    fun getBoostLevel(type: PoweredSystemType) = getPoweredSystem(type).boostLevel * boostLevelModifier

    fun startRepair(type: PoweredSystemType) {
        val system = getPoweredSystem(type)
        if (system.canRepair()) {
            system.startRepairing()

            stopRepairingAllExcept(type)
        }
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
            maxCapacitors = maxCapacitors,
            capacitorsPrediction = capacitorsPrediction(),
            settings = poweredSystems.mapValues { it.value.toMessage() }
        )

    private fun capacitorsPrediction(): Int? =
        when {
            powerGenerated > powerUsed && capacitors < maxCapacitors -> {
                (maxCapacitors - capacitors) / (powerGenerated - powerUsed)
            }
            powerGenerated < powerUsed && capacitors > 0.0 -> {
                -capacitors / (powerUsed - powerGenerated)
            }
            else -> {
                null
            }
        }?.roundToInt()

    private fun getPoweredSystem(type: PoweredSystemType) =
        poweredSystems[type] ?: PoweredSystem()

    private fun generatePower(time: GameTime) {
        val powerOutput = shipTemplate.reactorOutput * getPoweredSystem(PoweredSystemType.Reactor).boostLevel
        powerGenerated = powerOutput / 60
        capacitors += time.delta * powerGenerated
    }

    private fun drainPower(time: GameTime) {
        val powerUsage = poweredSystems
            .filter { it.key != PoweredSystemType.Reactor }
            .map { it.value.level }
            .sum()
            .toDouble()
        powerUsed = powerUsage / 60
        updateBoostLevelModifier(time.delta * powerUsed)
        capacitors -= time.delta * powerUsed
    }

    private fun updateBoostLevelModifier(powerUsed: Double) {
        boostLevelModifier = if (capacitors > 0.0) {
            if (powerUsed > capacitors) {
                capacitors / powerUsed
            } else {
                1.0
            }
        } else {
            0.0
        }
    }

    private fun finalizeCapacitorCharge() {
        capacitors = capacitors.clamp(0.0, maxCapacitors)
    }

    private fun updateHeatLevels(time: GameTime) {
        poweredSystems
            .forEach {
                it.value.update(time)
            }
    }

    private fun stopRepairingAllExcept(type: PoweredSystemType) {
        poweredSystems
            .filter {
                it.key != type
            }
            .forEach {
                it.value.stopRepairing()
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

        fun takeDamage(amount: Double) {
            damage += amount / shipTemplate.poweredSystemDamageCapacity
        }

        private fun updateHeat(time: GameTime) {
            val heatChangePerSecond = shipTemplate.heatBuildupBase.pow(ratio - 1.0) - (1.01 + coolant)
            val resultantHeat = heat + heatChangePerSecond * time.delta * shipTemplate.heatBuildupModifier
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

                if (currentRepairProgress >= 1.0) {
                    repairProgress = null

                    damage -= shipTemplate.repairAmount
                } else {
                    repairProgress = currentRepairProgress
                }
            }
        }

        fun canRepair() = repairProgress == null && damage > 0.0

        fun startRepairing() {
            if (canRepair()) {
                repairProgress = 0.0
            }
        }

        fun stopRepairing() {
            repairProgress = null
        }

        fun toMessage() =
            PoweredSystemMessage(
                repairProgress = repairProgress?.let {
                    RepairProgressMessage(
                        progress = it,
                        remainingTime = ((1.0 - it) / shipTemplate.repairSpeed).roundToInt()
                    )
                },
                damage = damage.fiveDigits(),
                level = level,
                heat = heat.fiveDigits(),
                coolant = coolant.fiveDigits()
            )
    }
}

typealias BoostLevel = () -> Double
