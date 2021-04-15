package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.TubeDirectionMessage
import de.stefanbissell.starcruiser.TubeMessage
import de.stefanbissell.starcruiser.TubeStatus
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.Vector3
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.ships.Magazine
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Tube
import de.stefanbissell.starcruiser.toRadians
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
                designation = "Tube 1",
                position = Vector3(2, 3, 4),
                direction = -45,
                velocity = 5.0
            ),
            Tube(
                designation = "Tube 2",
                position = Vector3(4, 3, 2),
                direction = 45,
                velocity = 5.0
            )
        ),
        magazine = Magazine(
            driveTemplate = TorpedoDriveTemplate(name = "TestDrive"),
            warheadTemplate = TorpedoWarheadTemplate(name = "TestWarhead")
        ),
        ship = ship
    )

    @Test
    fun `initially at full magazine capacity`() {
        expectThat(tubeHandlerContainer.magazineRemaining)
            .isEqualTo(Magazine().capacity)
    }

    @Test
    fun `starts reload for given tube`() {
        tubeHandlerContainer.requestReload(1)

        expectThat(tubeHandlerContainer.tubeHandlers[1])
            .get { status }
            .isEqualTo(TubeStatus.Reloading())
    }

    @Test
    fun `reduces magazine remaining on reload`() {
        tubeHandlerContainer.requestReload(1)

        expectThat(tubeHandlerContainer.magazineRemaining)
            .isEqualTo(Magazine().capacity - 1)
    }

    @Test
    fun `does not reload when magazine empty`() {
        tubeHandlerContainer.magazineRemaining = 1
        tubeHandlerContainer.requestReload(0)
        tubeHandlerContainer.requestReload(1)

        expectThat(tubeHandlerContainer.tubeHandlers[0])
            .get { status }
            .isEqualTo(TubeStatus.Reloading())
        expectThat(tubeHandlerContainer.tubeHandlers[1])
            .get { status }
            .isEqualTo(TubeStatus.Empty)
    }

    @Test
    fun `ignores invalid index for start reload call`() {
        tubeHandlerContainer.requestReload(-1)

        expectThat(tubeHandlerContainer.tubeHandlers)
            .all {
                get { status }.isEqualTo(TubeStatus.Empty)
            }
    }

    @Test
    fun `launches from given tube`() {
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Ready
        tubeHandlerContainer.requestLaunch(0)
        stepTime()

        expectThat(tubeHandlerContainer.tubeHandlers[0])
            .get { status }
            .isEqualTo(TubeStatus.Empty)
    }

    @Test
    fun `launch creates torpedo based on magazine settings`() {
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Ready
        tubeHandlerContainer.requestLaunch(0)
        stepTime()

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
                get { template.drive.name }.isEqualTo("TestDrive")
                get { template.warhead.name }.isEqualTo("TestWarhead")
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
        tubeHandlerContainer.requestLaunch(2)

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
                TubeMessage(
                    designation = "Tube 1",
                    status = TubeStatus.Ready
                ),
                TubeMessage(
                    designation = "Tube 2",
                    status = TubeStatus.Reloading(0.4)
                )
            )
        }
    }

    @Test
    fun `tube direction message on contains loaded tube`() {
        tubeHandlerContainer.tubeHandlers[0].status = TubeStatus.Empty
        tubeHandlerContainer.tubeHandlers[1].status = TubeStatus.Ready

        expectThat(tubeHandlerContainer.toDirectionMessage())
            .containsExactly(
                TubeDirectionMessage(
                    position = Vector3(4, 3, 2),
                    rotation = 45.0.toRadians()
                )
            )
    }

    private fun stepTime(seconds: Number = 0.1) {
        time.update(seconds)
        tubeHandlerContainer.update(time, 1.0)
    }
}
