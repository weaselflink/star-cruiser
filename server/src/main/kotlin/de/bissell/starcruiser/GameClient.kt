package de.bissell.starcruiser

import de.bissell.starcruiser.ThrottleMessage.*
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.*

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
            var lastSnapshot: SnapshotMessage? = null

            while (isActive) {
                if (throttleActor.getInflightMessageCount() < maxInflightMessages) {
                    val snapshot = gameStateActor.getGameStateSnapshot()
                    if (lastSnapshot != snapshot) {
                        val counterResponse = throttleActor.addInflightMessage()
                        lastSnapshot = snapshot
                        outgoing.sendText(
                            GameStateMessage(
                                counterResponse,
                                snapshot
                            ).toJson()
                        )
                    }
                }
                delay(updateIntervalMillis)
            }
        }

        for (frame in incoming) {
            val input = String(frame.data)
            when (val command = Command.parse(input)) {
                is Command.UpdateAcknowledge -> throttleActor.send(AcknowledgeInflightMessage(command.counter))
                is Command.CommandTogglePause -> gameStateActor.send(TogglePause)
                is Command.CommandSpawnShip -> gameStateActor.send(SpawnShip)
                is Command.CommandJoinShip -> gameStateActor.send(JoinShip(id, command.shipId.toUUID(), command.station))
                is Command.CommandChangeStation -> gameStateActor.send(ChangeStation(id, command.station))
                is Command.CommandExitShip -> gameStateActor.send(ExitShip(id))
                is Command.CommandChangeThrottle -> gameStateActor.send(ChangeThrottle(id, command.value))
                is Command.CommandChangeRudder -> gameStateActor.send(ChangeRudder(id, command.value))
                is Command.CommandAddWaypoint -> gameStateActor.send(AddWaypoint(id, command.position))
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

    private suspend fun SendChannel<GameStateChange>.getGameStateSnapshot(): SnapshotMessage {
        val response = CompletableDeferred<SnapshotMessage>()
        send(GetGameStateSnapshot(id, response))
        return response.await()
    }

    private fun String.toUUID() = UUID.fromString(this)

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

suspend fun SendChannel<Frame>.sendText(value: String) = send(Frame.Text(value))
