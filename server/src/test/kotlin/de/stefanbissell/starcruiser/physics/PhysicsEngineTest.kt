package de.stefanbissell.starcruiser.physics

import de.stefanbissell.starcruiser.Asteroid
import de.stefanbissell.starcruiser.p
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PhysicsEngineTest {

    private val physicsEngine = PhysicsEngine()

    @Test
    fun `finds obstruction`() {
        val asteroid = Asteroid().also {
            physicsEngine.addAsteroid(it)
        }

        expectThat(
            physicsEngine.findObstructions(
                p(-100, 0),
                p(100, 0)
            )
        ).isEqualTo(listOf(asteroid.id))
    }

    @Test
    fun `finds obstruction ignoring given ids`() {
        val asteroid1 = Asteroid().also {
            physicsEngine.addAsteroid(it)
        }
        val asteroid2 = Asteroid().also {
            it.position = p(50, 0)
            physicsEngine.addAsteroid(it)
        }

        expectThat(
            physicsEngine.findObstructions(
                start = p(-100, 0),
                end = p(100, 0),
                ignore = listOf(asteroid1.id)
            )
        ).isEqualTo(listOf(asteroid2.id))
    }
}
