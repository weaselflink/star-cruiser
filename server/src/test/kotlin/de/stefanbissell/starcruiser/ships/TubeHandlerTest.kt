package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.isNear
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class TubeHandlerTest {

    private val time = GameTime.atEpoch()
    private val launchTube = LaunchTube()
    private var power = 1.0
    private val ship = PlayerShip(faction = TestFactions.player)
    private val tubeHandler = TubeHandler(launchTube, ship)

    @Test
    fun `requesting launch on empty tube does nothing`() {
        tubeHandler.launch()

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Empty)
    }

    @Test
    fun `requesting launch on reloading tube does nothing`() {
        tubeHandler.status = TubeStatus.Reloading()
        tubeHandler.launch()

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Reloading())
    }

    @Test
    fun `requesting launch on ready tube empties it`() {
        tubeHandler.status = TubeStatus.Ready
        tubeHandler.launch()

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Empty)
    }

    @Test
    fun `requesting reload on empty tube starts reloading`() {
        tubeHandler.reload()

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Reloading())
    }

    @Test
    fun `requesting reload on reloading tube does nothing`() {
        tubeHandler.status = TubeStatus.Reloading(0.5)
        tubeHandler.reload()

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Reloading(0.5))
    }

    @Test
    fun `requesting reload on ready tube does nothing`() {
        tubeHandler.status = TubeStatus.Ready
        tubeHandler.reload()

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Ready)
    }

    @Test
    fun `updates reload progress`() {
        tubeHandler.status = TubeStatus.Reloading()
        stepTime(0.7 / launchTube.reloadSpeed)

        expectThat(tubeHandler.status)
            .isA<TubeStatus.Reloading>()
            .get { progress }.isNear(0.7)
    }

    @Test
    fun `reloading tube becomes ready`() {
        tubeHandler.status = TubeStatus.Reloading()
        stepTime(1.1 / launchTube.reloadSpeed)

        expectThat(tubeHandler.status)
            .isEqualTo(TubeStatus.Ready)
    }

    private fun stepTime(seconds: Number) {
        time.update(seconds.toDouble())
        tubeHandler.update(time, power)
    }
}
