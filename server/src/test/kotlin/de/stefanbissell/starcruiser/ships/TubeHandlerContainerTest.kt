package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.TubeStatus
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class TubeHandlerContainerTest {

    private val ship = NonPlayerShip(faction = TestFactions.neutral)
    private val tubeHandlerContainer = TubeHandlerContainer(
        launchTubes = listOf(LaunchTube(), LaunchTube()),
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

        expectThat(tubeHandlerContainer.tubeHandlers[0])
            .get { status }
            .isEqualTo(TubeStatus.Empty)
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
}
