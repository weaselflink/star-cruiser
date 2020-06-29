package de.bissell.starcruiser.client

import de.bissell.starcruiser.AddWaypoint
import de.bissell.starcruiser.ApplicationConfig.gameClientMaxInflightMessages
import de.bissell.starcruiser.ApplicationConfig.gameClientUpdateIntervalMillis
import de.bissell.starcruiser.ChangeJumpDistance
import de.bissell.starcruiser.ChangeRudder
import de.bissell.starcruiser.ChangeStation
import de.bissell.starcruiser.ChangeThrottle
import de.bissell.starcruiser.Command
import de.bissell.starcruiser.DeleteWaypoint
import de.bissell.starcruiser.ExitShip
import de.bissell.starcruiser.GameClientDisconnected
import de.bissell.starcruiser.GameStateChange
import de.bissell.starcruiser.GameStateMessage
import de.bissell.starcruiser.GetGameStateSnapshot
import de.bissell.starcruiser.JoinShip
import de.bissell.starcruiser.LockTarget
import de.bissell.starcruiser.NewGameClient
import de.bissell.starcruiser.ScanShip
import de.bissell.starcruiser.SetShieldsUp
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.SpawnShip
import de.bissell.starcruiser.StartJump
import de.bissell.starcruiser.TogglePause
import de.bissell.starcruiser.client.ThrottleMessage.AcknowledgeInflightMessage
import de.bissell.starcruiser.client.ThrottleMessage.AddInflightMessage
import de.bissell.starcruiser.client.ThrottleMessage.GetInflightMessageCount
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class ClientId(private val id: String) {

    companion object {
        fun random() = ClientId(UUID.randomUUID().toString())
    }
}

class GameClient(
    private val id: ClientId = ClientId.random(),
    private val gameStateActor: SendChannel<GameStateChange>,
    private val outgoing: SendChannel<Frame>,
    private val incoming: ReceiveChannel<Frame>
) {

    suspend fun start(coroutineScope: CoroutineScope) {
        val throttleActor = coroutineScope.createThrottleActor()
        gameStateActor.send(NewGameClient(id))

        val updateJob = coroutineScope.launchUpdateJob(throttleActor)

        for (frame in incoming) {
            val input = String(frame.data)
            when (val command = Command.parse(input)) {
                is Command.UpdateAcknowledge -> throttleActor.send(AcknowledgeInflightMessage(command.counter))
                is Command.CommandTogglePause -> gameStateActor.send(
                    TogglePause
                )
                is Command.CommandSpawnShip -> gameStateActor.send(
                    SpawnShip
                )
                is Command.CommandJoinShip -> gameStateActor.send(
                    JoinShip(id, command.objectId, command.station)
                )
                is Command.CommandChangeStation -> gameStateActor.send(
                    ChangeStation(id, command.station)
                )
                is Command.CommandExitShip -> gameStateActor.send(
                    ExitShip(id)
                )
                is Command.CommandChangeThrottle -> gameStateActor.send(
                    ChangeThrottle(id, command.value)
                )
                is Command.CommandChangeJumpDistance -> gameStateActor.send(
                    ChangeJumpDistance(id, command.value)
                )
                is Command.CommandStartJump -> gameStateActor.send(
                    StartJump(id)
                )
                is Command.CommandChangeRudder -> gameStateActor.send(
                    ChangeRudder(id, command.value)
                )
                is Command.CommandAddWaypoint -> gameStateActor.send(
                    AddWaypoint(id, command.position)
                )
                is Command.CommandDeleteWaypoint -> gameStateActor.send(
                    DeleteWaypoint(id, command.index)
                )
                is Command.CommandScanShip -> gameStateActor.send(
                    ScanShip(id, command.targetId)
                )
                is Command.CommandLockTarget -> gameStateActor.send(
                    LockTarget(id, command.targetId)
                )
                is Command.CommandSetShieldsUp -> gameStateActor.send(
                    SetShieldsUp(id, command.value)
                )
            }
        }

        updateJob.cancelAndJoin()
        gameStateActor.send(GameClientDisconnected(id))
    }

    private fun CoroutineScope.launchUpdateJob(throttleActor: SendChannel<ThrottleMessage>): Job {
        return launch {
            var lastSnapshot: SnapshotMessage? = null

            while (isActive) {
                if (throttleActor.getInflightMessageCount() < gameClientMaxInflightMessages) {
                    val snapshot = gameStateActor.getGameStateSnapshot()
                    if (lastSnapshot != snapshot) {
                        val counterResponse = throttleActor.addInflightMessage()
                        lastSnapshot = snapshot
                        outgoing.send(
                            GameStateMessage(
                                counterResponse,
                                snapshot
                            )
                        )
                    }
                }
                delay(gameClientUpdateIntervalMillis)
            }
        }
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

    private suspend fun SendChannel<Frame>.send(message: GameStateMessage) = send(Frame.Text(message.toJson()))

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
