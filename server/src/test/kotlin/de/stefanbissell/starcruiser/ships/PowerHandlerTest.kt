package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.PoweredSystemType.Shields
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
        powerHandler.setLevel(Shields, 50)
        expectThat(getLevel(Shields)).isEqualTo(50)
    }

    @Test
    fun `caps power above 0`() {
        powerHandler.setLevel(Shields, -10)
        expectThat(getLevel(Shields)).isEqualTo(0)
    }

    @Test
    fun `caps power below 200`() {
        powerHandler.setLevel(Shields, 210)
        expectThat(getLevel(Shields)).isEqualTo(200)
    }

    @Test
    fun `rounds power to nearest multiple of 5`() {
        powerHandler.setLevel(Shields, 52)
        expectThat(getLevel(Shields)).isEqualTo(50)
        powerHandler.setLevel(Shields, 58)
        expectThat(getLevel(Shields)).isEqualTo(60)
    }

    @Test
    fun `compares messages correctly`() {
        val initialMessage = powerHandler.toMessage()

        powerHandler.setLevel(Shields, 52)

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

    private fun getLevel(systemType: PoweredSystemType) =
        powerHandler.toMessage().settings[systemType]?.level
}
