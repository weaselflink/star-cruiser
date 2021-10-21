package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.ExitShip
import de.stefanbissell.starcruiser.GameStateChange
import de.stefanbissell.starcruiser.GameStateMessage
import de.stefanbissell.starcruiser.GetGameStateSnapshot
import de.stefanbissell.starcruiser.NewGameClient
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.expectWithTimeout
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger.NOP_LOGGER
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.one

class GameClientTest {

    private val gameStateActor = Channel<GameStateChange>()
    private val statisticsActor = Channel<StatisticsMessage>()
    private val outgoing = Channel<Frame>()
    private val incoming = Channel<Frame>()
    private val clientId = ClientId.random()
    private val gameClient = GameClient(
        id = clientId,
        gameStateActor = gameStateActor,
        statisticsActor = statisticsActor,
        outgoing = outgoing,
        incoming = incoming,
        log = NOP_LOGGER
    )

    private val gameStateChanges = mutableListOf<GameStateChange>()
    private val statistics = mutableListOf<StatisticsMessage>()
    private val messages = mutableListOf<Frame>()

    @Test
    fun `send new game client message to game state`() {
        withGameClient {
            expectWithTimeout {
                expectThat(gameStateChanges).one {
                    isEqualTo(NewGameClient(clientId))
                }
            }
        }
    }

    @Test
    fun `requests game state`() {
        withGameClient {
            expectWithTimeout {
                expectThat(gameStateChanges).one {
                    isA<GetGameStateSnapshot>()
                        .get { clientId }.isEqualTo(clientId)
                }
            }
        }
    }

    @Test
    fun `sends update`() {
        val update = SnapshotMessage.ShipSelection(emptyList())
        withGameClient {
            expectWithTimeout {
                val request = gameStateChanges
                    .filterIsInstance<GetGameStateSnapshot>()
                    .firstOrNull { it.clientId == clientId }
                expectThat(request).isNotNull()

                request!!.response.complete(update)
            }
            expectWithTimeout {
                expectThat(messages).one {
                    expectGameStateMessage()
                        .isEqualTo(GameStateMessage(1L, update))
                }
            }
        }
    }

    @Test
    fun `modifies game state on incoming command`() {
        withGameClient {
            launch {
                sendCommand(Command.CommandExitShip)
            }
            expectWithTimeout {
                expectThat(gameStateChanges).one {
                    isEqualTo(ExitShip(clientId))
                }
            }
        }
    }

    @Test
    fun `skips invalid command`() {
        withGameClient {
            launch {
                incoming.send(Frame.Text("invalid"))
            }
            launch {
                sendCommand(Command.CommandExitShip)
            }
            expectWithTimeout {
                expectThat(gameStateChanges).one {
                    isEqualTo(ExitShip(clientId))
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun withGameClient(block: suspend CoroutineScope.() -> Unit) {
        runBlockingTest {
            val supervisorJob = SupervisorJob()
            launch(supervisorJob) {
                for (gameStateChange in gameStateActor) {
                    gameStateChanges += gameStateChange
                }
            }
            launch(supervisorJob) {
                for (statistic in statisticsActor) {
                    statistics += statistic
                }
            }
            launch(supervisorJob) {
                for (message in outgoing) {
                    messages += message
                }
            }
            launch(supervisorJob) {
                gameClient.start(this)
            }

            try {
                withContext(supervisorJob) {
                    block()
                }
            } finally {
                supervisorJob.complete()
            }
        }
    }

    private suspend fun sendCommand(command: Command) {
        incoming.send(Frame.Text(command.toJson()))
    }

    private fun Assertion.Builder<Frame>.expectGameStateMessage() =
        get { GameStateMessage.parse(String(data)) }
}
