package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.RepairProgressMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.minigames.CircuitPathGame
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
    private var repairHandler: RepairHandler? = null

    fun update(time: GameTime) {
        generatePower(time)
        drainPower(time)
        finalizeCapacitorCharge()
        updateHeatLevels(time)
        updateRepairHandler(time)
    }

    fun takeDamage(type: PoweredSystemType, amount: Double) {
        getPoweredSystem(type).takeDamage(amount)
    }

    fun getBoostLevel(type: PoweredSystemType) = getPoweredSystem(type).boostLevel * boostLevelModifier

    fun startRepair(type: PoweredSystemType) {
        val system = getPoweredSystem(type)
        if (system.canRepair() && repairHandler?.type != type) {
            repairHandler = RepairHandler(type)
        }
    }

    fun abortRepair() {
        repairHandler = null
    }

    fun solveRepairGame(column: Int, row: Int) {
        repairHandler?.apply {
            game.rotateTile(column, row)
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
            settings = poweredSystems.mapValues { it.value.toMessage() },
            repairProgress = repairHandler?.toMessage()
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

    private fun updateRepairHandler(time: GameTime) {
        repairHandler?.apply {
            update(time)
            if (solvedTimer > 1.0) {
                getPoweredSystem(type).apply {
                    damage -= shipTemplate.repairAmount
                }
                repairHandler = null
            }
        }
    }

    inner class PoweredSystem {

        var damage: Double = 0.0
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

        fun canRepair() = damage > 0.0

        fun toMessage() =
            PoweredSystemMessage(
                damage = damage.fiveDigits(),
                level = level,
                heat = heat.fiveDigits(),
                coolant = coolant.fiveDigits()
            )
    }
}

private class RepairHandler(
    val type: PoweredSystemType
) {
    var solvedTimer = 0.0
    val game = CircuitPathGame.createUnsolved(8, 3)

    fun update(time: GameTime) {
        if (game.isSolved) {
            solvedTimer += time.delta
        } else {
            solvedTimer = 0.0
        }
    }

    fun toMessage() =
        RepairProgressMessage(
            type = type,
            width = game.width,
            height = game.height,
            start = game.start.second,
            end = game.end.second,
            tiles = game.encodedTiles
        )
}

typealias BoostLevel = () -> Double
