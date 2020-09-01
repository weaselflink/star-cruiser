package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.GameStateChange
import de.stefanbissell.starcruiser.GetGameStateSnapshot
import de.stefanbissell.starcruiser.NewGameClient
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.one

class GameClientTest {

    private val gameStateActor = Channel<GameStateChange>()
    private val statisticsActor = Channel<StatisticsMessage>()
    private val outgoing = Channel<Frame>()
    private val incoming = Channel<Frame>()
    private val gameClient = GameClient(
        gameStateActor = gameStateActor,
        statisticsActor = statisticsActor,
        outgoing = outgoing,
        incoming = incoming
    )

    private val gameStateChanges = mutableListOf<GameStateChange>()
    private val statistics = mutableListOf<StatisticsMessage>()
    private val messages = mutableListOf<Frame>()

    @Test
    fun `send new game client message to game state`() {
        withGameClient {
            expectWithTimeout(1000) {
                expectThat(gameStateChanges).one {
                    isA<NewGameClient>()
                }
            }
        }
    }

    @Test
    fun `requests game state`() {
        withGameClient {
            expectWithTimeout(1000) {
                expectThat(gameStateChanges).one {
                    isA<GetGameStateSnapshot>()
                }
            }
        }
    }

    private fun withGameClient(block: suspend () -> Unit) {
        runBlocking {
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
                block()
            } finally {
                supervisorJob.complete()
            }
        }
    }

    private suspend fun expectWithTimeout(timeMillis: Long, block: suspend () -> Unit) {
        withTimeout(timeMillis) {
            var waiting = true
            while (waiting) {
                try {
                    block()
                    waiting = false
                } catch (ignore: Error) {}
                delay(100)
            }
        }
    }
}
