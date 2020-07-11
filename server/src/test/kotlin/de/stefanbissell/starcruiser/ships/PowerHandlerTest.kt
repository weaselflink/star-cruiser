package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.PoweredSystem
import de.stefanbissell.starcruiser.PoweredSystem.Shields
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PowerHandlerTest {

    private val powerHandler = PowerHandler()

    @Test
    fun `holds initial value for each system`() {
        PoweredSystem.values().forEach {
            expectThat(powerHandler[it]).isEqualTo(100)
        }
    }

    @Test
    fun `can set system power`() {
        powerHandler[Shields] = 50
        expectThat(powerHandler[Shields]).isEqualTo(50)
    }

    @Test
    fun `caps power above 0`() {
        powerHandler[Shields] = -10
        expectThat(powerHandler[Shields]).isEqualTo(0)
    }

    @Test
    fun `caps power below 200`() {
        powerHandler[Shields] = 210
        expectThat(powerHandler[Shields]).isEqualTo(200)
    }

    @Test
    fun `rounds power to nearest multiple of 5`() {
        powerHandler[Shields] = 52
        expectThat(powerHandler[Shields]).isEqualTo(50)
        powerHandler[Shields] = 58
        expectThat(powerHandler[Shields]).isEqualTo(60)
    }
}
