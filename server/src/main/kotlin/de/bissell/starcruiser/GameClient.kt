package de.bissell.starcruiser

import de.bissell.starcruiser.ThrottleMessage.*
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.*

@ObsoleteCoroutinesApi
class GameClient(
    private val id: UUID = UUID.randomUUID(),
    private val gameStateActor: SendChannel<GameStateChange>,
    private val outgoing: SendChannel<Frame>,
    private val incoming: ReceiveChannel<Frame>
) {

    private val maxInflightMessages: Int = 3
    private val updateIntervalMillis: Long = 10

    suspend fun start(coroutineScope: CoroutineScope) {
        val throttleActor = coroutineScope.createThrottleActor()
        gameStateActor.send(NewGameClient(id))

        val updateJob = coroutineScope.launch {
            while (isActive) {
                if (throttleActor.getInflightMessageCount() < maxInflightMessages) {
                    val counterResponse = throttleActor.addInflightMessage()
                    val gameStateSnapShot = gameStateActor.getGameStateSnapShot()
                    outgoing.sendText(
                        GameStateMessage(
                            counterResponse,
                            gameStateSnapShot
                        ).toJson()
                    )
                }
                delay(updateIntervalMillis)
            }
        }

        for (frame in incoming) {
            val input = String(frame.data)
            when (val command = Command.parse(input)) {
                is Command.UpdateAcknowledge -> throttleActor.send(AcknowledgeInflightMessage(command.counter))
                is Command.CommandTogglePause -> gameStateActor.send(TogglePause)
                is Command.CommandSpawnShip -> gameStateActor.send(TogglePause)
                is Command.CommandJoinShip -> gameStateActor.send(JoinShip(id, UUID.fromString(command.shipId)))
                is Command.CommandExitShip -> gameStateActor.send(ExitShip(id))
                is Command.CommandChangeThrottle -> gameStateActor.send(ChangeThrottle(id, command.value))
                is Command.CommandChangeRudder -> gameStateActor.send(ChangeRudder(id, command.diff))
            }
        }

        updateJob.cancelAndJoin()
        gameStateActor.send(GameClientDisconnected(id))
    }

    private suspend fun SendChannel<ThrottleMessage>.getInflightMessageCount(): Int {
        val response = CompletableDeferred<Int>()
        send(GetInflightMessageCount(response))
        return response.await()
    }

    private suspend fun SendChannel<ThrottleMessage>.addInflightMessage(): Long {
        val response = CompletableDeferred<Long>()
        send(AddInflightMessage(response))
        return response.await()
    }

    private suspend fun SendChannel<GameStateChange>.getGameStateSnapShot(): GameStateSnapshot {
        val response = CompletableDeferred<GameStateSnapshot>()
        send(GetGameStateSnapshot(id, response))
        return response.await()
    }

    companion object {
        suspend fun CoroutineScope.startGameClient(
            gameStateActor: SendChannel<GameStateChange>,
            outgoing: SendChannel<Frame>,
            incoming: ReceiveChannel<Frame>
        ) =
            GameClient(
                gameStateActor = gameStateActor,
                outgoing = outgoing,
                incoming = incoming
            ).start(this)
    }
}
