package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.TestFactions
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isEqualTo

class TubeHandlerContainerTest {

    private val ship = NonPlayerShip(faction = TestFactions.neutral)
    private val tubeHandlerContainer = TubeHandlerContainer(
        listOf(LaunchTube(), LaunchTube()), Magazine(), ship
    )

    @Test
    fun `starts reload for given tube`() {
        tubeHandlerContainer.startReload(1)

        expectThat(tubeHandlerContainer.tubeHandlers[1])
            .get { status }
            .isEqualTo(TubeStatus.Reloading())
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
}
