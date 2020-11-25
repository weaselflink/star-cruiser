package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.TubeStatus
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.Vector3
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.ships.Magazine
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Tube
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.withFirst
import kotlin.math.PI

class TubeHandlerContainerTest {

    private val time = GameTime.atEpoch()
    private val ship = NonPlayerShip(
        faction = TestFactions.neutral,
        position = Vector2(100, 200),
        rotation = PI
    )
    private val tubeHandlerContainer = TubeHandlerContainer(
        tubes = listOf(
            Tube(
                position = Vector3(2, 3, 4),
                direction = -45,
                velocity = 5.0
            ),
            Tube()
        ),
        magazine = Magazine(),
        ship = ship
    )

    @Test
    fun `initially at full magazine capacity`() {
        expectThat(tubeHandlerContainer.magazineRemaining)
            .isEqualTo(Magazine().capacity)
    }

    @Test
    fun `starts reload for given tube`() {
        tubeHandlerContainer.startReload(1)

        expectThat(tubeHandlerContainer.tubeHandlers[1])
            .get { status }
            .isEqualTo(TubeStatus.Reloading())
    }

    @Test
    fun `reduces magazine remaining on reload`() {
        tubeHandlerContainer.startReload(1)

        expectThat(tubeHandlerContainer.magazineRemaining)
            .isEqualTo(Magazine().capacity - 1)
    }

    @Test
    fun `does not reload when magazine empty`() {
        tubeHandlerContainer.magazineRemaining = 1
        tubeHandlerContainer.startReload(0)
        tubeHandlerContainer.startReload(1)

        expectThat(tubeHandlerContainer.tubeHandlers[0])
            .get { status }
            .isEqualTo(TubeStatus.Reloading())
        expectThat(tubeHandlerContainer.tubeHandlers[1])
            .get { status }
            .isEqualTo(TubeStatus.Empty)
    }

    @Test
    fun `ignores invalid index for start reload call`() {
        tubeHandlerContainer.startReload(-1)

        expectThat(tubeHandlerContainer.tubeHandlers)
            .all {
                get { status }.isEqualTo(TubeStatus.Empty)
            }
    }

    @Test
    fun `launches from given tube`() {
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Ready
        tubeHandlerContainer.launch(0)
        stepTime(0.1)

        expectThat(tubeHandlerContainer.tubeHandlers[0])
            .get { status }
            .isEqualTo(TubeStatus.Empty)
    }

    @Test
    fun `launch creates torpedo`() {
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Ready
        tubeHandlerContainer.launch(0)
        stepTime(0.1)

        val expectedRotation = PI * 0.75
        expectThat(tubeHandlerContainer.torpedoes)
            .hasSize(1)
            .withFirst {
                get { position }.isNear(
                    Vector2(100, 200) +
                        Vector2(2, 3).rotate(expectedRotation)
                )
                get { rotation }.isNear(expectedRotation)
                get { speed }.isNear(
                    Vector2(5, 0).rotate(expectedRotation)
                )
            }
    }

    @Test
    fun `creates no torpedoes without launch`() {
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Ready

        expectThat(tubeHandlerContainer.torpedoes)
            .isEmpty()
    }

    @Test
    fun `ignores invalid index for launch call`() {
        tubeHandlerContainer.tubeHandlers.forEach {
            it.status = TubeStatus.Ready
        }
        tubeHandlerContainer.launch(2)

        expectThat(tubeHandlerContainer.tubeHandlers)
            .all {
                get { status }.isEqualTo(TubeStatus.Ready)
            }
    }

    @Test
    fun `creates message for client`() {
        tubeHandlerContainer.magazineRemaining = 10
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Ready
        tubeHandlerContainer.tubeHandlers[1].status = TubeStatus.Reloading(0.4)

        expectThat(tubeHandlerContainer.toMessage()) {
            get { magazineMax }.isEqualTo(Magazine().capacity)
            get { magazineRemaining }.isEqualTo(10)
            get { tubes }.containsExactly(
                TubeStatus.Ready,
                TubeStatus.Reloading(0.4)
            )
        }
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        tubeHandlerContainer.update(time, 1.0)
    }
}
