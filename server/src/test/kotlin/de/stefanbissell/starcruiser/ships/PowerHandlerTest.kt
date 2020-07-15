package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.PoweredSystemType.Impulse
import de.stefanbissell.starcruiser.PoweredSystemType.Jump
import de.stefanbissell.starcruiser.PoweredSystemType.Maneuver
import de.stefanbissell.starcruiser.PoweredSystemType.Reactor
import de.stefanbissell.starcruiser.PoweredSystemType.Shields
import de.stefanbissell.starcruiser.PoweredSystemType.Weapons
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import java.time.Instant

class PowerHandlerTest {

    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val shipTemplate = ShipTemplate()
    private val powerHandler = PowerHandler(shipTemplate)

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
            .isEqualTo(shipTemplate.maxCapacitors)

        stepTimeToOneMinute()

        expectThat(powerHandler.toMessage().capacitors)
            .isNear(shipTemplate.maxCapacitors - 300.0)
    }

    private fun stepTimeToOneMinute() {
        time.update(Instant.EPOCH.plusMillis((60.toDouble() * 1000).toLong()))
        powerHandler.update(time)
    }

    private fun setLevel(systemType: PoweredSystemType, value: Int) =
        powerHandler.setLevel(systemType, value)

    private fun getLevel(systemType: PoweredSystemType) =
        powerHandler.toMessage().settings[systemType]?.level
}
