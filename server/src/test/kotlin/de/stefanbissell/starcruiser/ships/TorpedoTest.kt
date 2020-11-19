package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.physics.BodyParameters
import de.stefanbissell.starcruiser.physics.PhysicsEngine
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class TorpedoTest {

    private val time = GameTime.atEpoch()
    private val maxBurnTime = 10.0
    private val thrust = 20.0
    private val physicsEngine = mockk<PhysicsEngine>(relaxed = true)
    private val torpedo = Torpedo(
        launcherId = ObjectId.random(),
        faction = TestFactions.player,
        position = p(3, -4),
        thrust = thrust,
        maxBurnTime = maxBurnTime
    )

    @BeforeEach
    fun setUp() {
        every {
            physicsEngine.getBodyParameters(torpedo.id)
        } returns BodyParameters(
            position = Vector2(3, -4)
        )
    }

    @Test
    fun `not destroyed before max burn time`() {
        expectThat(stepTime(maxBurnTime - 1).destroyed)
            .isFalse()
    }

    @Test
    fun `destroyed after max burn time`() {
        expectThat(stepTime(maxBurnTime + 1).destroyed)
            .isTrue()
    }

    @Test
    fun `updates physics engine`() {
        stepTime(1)

        verify(exactly = 1) {
            physicsEngine.updateObject(
                torpedo.id,
                thrust
            )
        }
    }

    @Test
    fun `destroyed on taking damage`() {
        torpedo.takeDamage(PoweredSystemType.Shields, 0.1, 0)

        expectThat(stepTime(1).destroyed)
            .isTrue()
    }

    private fun stepTime(seconds: Number): ShipUpdateResult {
        time.update(seconds.toDouble())
        torpedo.update(time, physicsEngine)
        return torpedo.endUpdate()
    }
}
