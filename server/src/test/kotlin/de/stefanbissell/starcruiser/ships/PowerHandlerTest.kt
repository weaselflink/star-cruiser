package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.PoweredSystemType.Impulse
import de.stefanbissell.starcruiser.PoweredSystemType.Jump
import de.stefanbissell.starcruiser.PoweredSystemType.Maneuver
import de.stefanbissell.starcruiser.PoweredSystemType.Reactor
import de.stefanbissell.starcruiser.PoweredSystemType.Sensors
import de.stefanbissell.starcruiser.PoweredSystemType.Shields
import de.stefanbissell.starcruiser.PoweredSystemType.Weapons
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class PowerHandlerTest {

    private val time = GameTime.atEpoch()
    private val powerHandler = PowerHandler(carrierTemplate)

    @Test
    fun `holds initial value for each system`() {
        PoweredSystemType.values().forEach {
            if (it == Reactor) {
                expectThat(it.getLevel()).isEqualTo(200)
                expectThat(it.getCoolant()).isEqualTo(1.0)
            } else {
                expectThat(it.getLevel()).isEqualTo(100)
                expectThat(it.getCoolant()).isEqualTo(0.0)
            }
        }
    }

    @Test
    fun `can set system power`() {
        Maneuver.setLevel(50)
        expectThat(Maneuver.getLevel()).isEqualTo(50)
    }

    @Test
    fun `caps power above 0`() {
        Weapons.setLevel(-10)
        expectThat(Weapons.getLevel()).isEqualTo(0)
    }

    @Test
    fun `caps power below 200`() {
        Shields.setLevel(210)
        expectThat(Shields.getLevel()).isEqualTo(200)
    }

    @Test
    fun `rounds power to nearest multiple of 5`() {
        Jump.setLevel(52)
        expectThat(Jump.getLevel()).isEqualTo(50)
        Impulse.setLevel(58)
        expectThat(Impulse.getLevel()).isEqualTo(60)
    }

    @Test
    fun `compares messages correctly`() {
        val initialMessage = powerHandler.toMessage()

        Reactor.setLevel(52)

        expectThat(powerHandler.toMessage())
            .isNotEqualTo(initialMessage)
    }

    @Test
    fun `updates capacitors`() {
        Reactor.setLevel(100)
        expectThat(powerHandler.toMessage().capacitors)
            .isEqualTo(carrierTemplate.maxCapacitors)

        stepTime(60)

        expectThat(powerHandler.toMessage().capacitors)
            .isNear(carrierTemplate.maxCapacitors - 300.0)
    }

    @Test
    fun `updates boost level modifier`() {
        Reactor.setLevel(100)
        stepTime(198)

        expectThat(powerHandler.toMessage().capacitors)
            .isNear(10.0)

        stepTime(10)

        expectThat(powerHandler.getBoostLevel(Weapons))
            .isEqualTo(0.6)
        expectThat(powerHandler.toMessage().capacitors)
            .isEqualTo(0.0)

        stepTime(10)

        expectThat(powerHandler.getBoostLevel(Weapons))
            .isEqualTo(0.5)
        expectThat(powerHandler.toMessage().capacitors)
            .isEqualTo(0.0)
    }

    @Test
    fun `starts repair progress`() {
        powerHandler.takeDamage(Jump, 2.5)
        powerHandler.startRepair(Jump)

        expectThat(
            powerHandler.toMessage().repairProgress
        ).isNotNull()
            .get { type }.isEqualTo(Jump)
    }

    @Test
    fun `does not start repair progress if not needed`() {
        powerHandler.startRepair(Jump)

        expectThat(
            powerHandler.toMessage().repairProgress
        ).isNull()
    }

    @Test
    fun `predicts time to capacitors empty`() {
        powerHandler.setLevel(Weapons, 0)
        powerHandler.setLevel(Shields, 0)
        powerHandler.setLevel(Jump, 0)

        stepTime(1)

        expectThat(powerHandler.toMessage().capacitorsPrediction)
            .isNull()
    }

    @Test
    fun `show stable capacitors`() {
        Reactor.setLevel(100)
        stepTime(1)

        expectThat(powerHandler.toMessage().capacitorsPrediction)
            .isNotNull()
            .isEqualTo(-199)
    }

    @Test
    fun `predicts time to capacitors full`() {
        Reactor.setLevel(100)
        stepTime(60)

        expectThat(powerHandler.toMessage().capacitors)
            .isNear(700.0)

        powerHandler.setLevel(Weapons, 0)
        powerHandler.setLevel(Shields, 0)
        powerHandler.setLevel(Jump, 0)
        powerHandler.setLevel(Sensors, 0)

        stepTime(60)

        expectThat(powerHandler.toMessage().capacitorsPrediction)
            .isNotNull()
            .isEqualTo(120)
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        powerHandler.update(time)
    }

    private fun PoweredSystemType.setLevel(value: Int) =
        powerHandler.setLevel(this, value)

    private fun PoweredSystemType.getLevel() =
        powerHandler.toMessage().settings[this]?.level

    private fun PoweredSystemType.getCoolant() =
        powerHandler.toMessage().settings[this]?.coolant
}
