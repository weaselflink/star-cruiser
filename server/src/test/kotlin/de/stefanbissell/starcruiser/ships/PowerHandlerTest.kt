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
import java.time.Instant

class PowerHandlerTest {

    private val time = GameTime(Instant.EPOCH)
    private val powerHandler = PowerHandler(carrierTemplate)

    @Test
    fun `holds initial value for each system`() {
        PoweredSystemType.values().forEach {
            expectThat(getLevel(it)).isEqualTo(100)
        }
    }

    @Test
    fun `can set system power`() {
        setLevel(Maneuver, 50)
        expectThat(getLevel(Maneuver)).isEqualTo(50)
    }

    @Test
    fun `caps power above 0`() {
        setLevel(Weapons, -10)
        expectThat(getLevel(Weapons)).isEqualTo(0)
    }

    @Test
    fun `caps power below 200`() {
        setLevel(Shields, 210)
        expectThat(getLevel(Shields)).isEqualTo(200)
    }

    @Test
    fun `rounds power to nearest multiple of 5`() {
        setLevel(Jump, 52)
        expectThat(getLevel(Jump)).isEqualTo(50)
        setLevel(Impulse, 58)
        expectThat(getLevel(Impulse)).isEqualTo(60)
    }

    @Test
    fun `compares messages correctly`() {
        val initialMessage = powerHandler.toMessage()

        setLevel(Reactor, 52)

        expectThat(powerHandler.toMessage())
            .isNotEqualTo(initialMessage)
    }

    @Test
    fun `updates capacitors`() {
        expectThat(powerHandler.toMessage().capacitors)
            .isEqualTo(carrierTemplate.maxCapacitors)

        stepTime(60)

        expectThat(powerHandler.toMessage().capacitors)
            .isNear(carrierTemplate.maxCapacitors - 300.0)
    }

    @Test
    fun `updates boost level modifier`() {
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
        stepTime(1)

        expectThat(powerHandler.toMessage().capacitorsPrediction)
            .isNotNull()
            .isEqualTo(-199)
    }

    @Test
    fun `predicts time to capacitors full`() {
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

    private fun setLevel(systemType: PoweredSystemType, value: Int) =
        powerHandler.setLevel(systemType, value)

    private fun getLevel(systemType: PoweredSystemType) =
        powerHandler.toMessage().settings[systemType]?.level
}
