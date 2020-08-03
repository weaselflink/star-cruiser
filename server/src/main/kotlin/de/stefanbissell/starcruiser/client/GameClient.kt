package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.AddWaypoint
import de.stefanbissell.starcruiser.ApplicationConfig.gameClientMaxInflightMessages
import de.stefanbissell.starcruiser.ApplicationConfig.gameClientUpdateIntervalMillis
import de.stefanbissell.starcruiser.ChangeJumpDistance
import de.stefanbissell.starcruiser.ChangeRudder
import de.stefanbissell.starcruiser.ChangeStation
import de.stefanbissell.starcruiser.ChangeThrottle
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.DeleteWaypoint
import de.stefanbissell.starcruiser.ExitShip
import de.stefanbissell.starcruiser.GameClientDisconnected
import de.stefanbissell.starcruiser.GameStateChange
import de.stefanbissell.starcruiser.GameStateMessage
import de.stefanbissell.starcruiser.GetGameStateSnapshot
import de.stefanbissell.starcruiser.JoinShip
import de.stefanbissell.starcruiser.LockTarget
import de.stefanbissell.starcruiser.NewGameClient
import de.stefanbissell.starcruiser.ScanShip
import de.stefanbissell.starcruiser.SetCoolant
import de.stefanbissell.starcruiser.SetMainScreenView
import de.stefanbissell.starcruiser.SetPower
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.SpawnShip
import de.stefanbissell.starcruiser.StartJump
import de.stefanbissell.starcruiser.StartRepair
import de.stefanbissell.starcruiser.TogglePause
import de.stefanbissell.starcruiser.ToggleShieldsUp
import de.stefanbissell.starcruiser.client.ThrottleMessage.AcknowledgeInflightMessage
import de.stefanbissell.starcruiser.client.ThrottleMessage.AddInflightMessage
import de.stefanbissell.starcruiser.client.ThrottleMessage.GetInflightMessageCount
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
    private val statisticsActor: SendChannel<StatisticsMessage>,
    private val outgoing: SendChannel<Frame>,
    private val incoming: ReceiveChannel<Frame>
) {

    suspend fun start(coroutineScope: CoroutineScope) {
        val throttleActor = coroutineScope.createThrottleActor()
        gameStateActor.send(NewGameClient(id))

        val updateJob = coroutineScope.launchUpdateJob(throttleActor, statisticsActor)

        for (frame in incoming) {
            statisticsActor.send(StatisticsMessage.MessageReceived(frame.data.size))
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
                is Command.CommandToggleShieldsUp -> gameStateActor.send(
                    ToggleShieldsUp(id)
                )
                is Command.CommandRepair -> gameStateActor.send(
                    StartRepair(id, command.systemType)
                )
                is Command.CommandSetPower -> gameStateActor.send(
                    SetPower(id, command.systemType, command.power)
                )
                is Command.CommandSetCoolant -> gameStateActor.send(
                    SetCoolant(id, command.systemType, command.coolant)
                )
                is Command.CommandMainScreenView -> gameStateActor.send(
                    SetMainScreenView(id, command.mainScreenView)
                )
            }
        }

        updateJob.cancelAndJoin()
        gameStateActor.send(GameClientDisconnected(id))
    }

    private fun CoroutineScope.launchUpdateJob(
        throttleActor: SendChannel<ThrottleMessage>,
        statisticsActor: SendChannel<StatisticsMessage>
    ): Job {
        return launch {
            var lastSnapshot: SnapshotMessage? = null

            while (isActive) {
                if (throttleActor.getInflightMessageCount() < gameClientMaxInflightMessages) {
                    val snapshot = gameStateActor.getGameStateSnapshot()
                    if (lastSnapshot != snapshot) {
                        val counterResponse = throttleActor.addInflightMessage()
                        lastSnapshot = snapshot
                        val message = GameStateMessage(
                            counterResponse,
                            snapshot
                        )
                        outgoing.send(message)
                        statisticsActor.send(StatisticsMessage.MessageSent(Frame.Text(message.toJson()).data.size))
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
            statisticsActor: SendChannel<StatisticsMessage>,
            outgoing: SendChannel<Frame>,
            incoming: ReceiveChannel<Frame>
        ) =
            GameClient(
                gameStateActor = gameStateActor,
                statisticsActor = statisticsActor,
                outgoing = outgoing,
                incoming = incoming
            ).start(this)
    }
}
