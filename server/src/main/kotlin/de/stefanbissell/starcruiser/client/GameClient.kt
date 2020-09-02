package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.AbortRepair
import de.stefanbissell.starcruiser.AbortScan
import de.stefanbissell.starcruiser.AddWaypoint
import de.stefanbissell.starcruiser.ApplicationConfig.gameClientMaxInflightMessages
import de.stefanbissell.starcruiser.ApplicationConfig.gameClientUpdateIntervalMillis
import de.stefanbissell.starcruiser.ChangeJumpDistance
import de.stefanbissell.starcruiser.ChangeRudder
import de.stefanbissell.starcruiser.ChangeStation
import de.stefanbissell.starcruiser.ChangeThrottle
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.DeleteSelectedWaypoint
import de.stefanbissell.starcruiser.ExitShip
import de.stefanbissell.starcruiser.GameClientDisconnected
import de.stefanbissell.starcruiser.GameStateChange
import de.stefanbissell.starcruiser.GameStateMessage
import de.stefanbissell.starcruiser.GetGameStateSnapshot
import de.stefanbissell.starcruiser.JoinShip
import de.stefanbissell.starcruiser.LockTarget
import de.stefanbissell.starcruiser.MapClearSelection
import de.stefanbissell.starcruiser.MapSelectShip
import de.stefanbissell.starcruiser.MapSelectWaypoint
import de.stefanbissell.starcruiser.NewGameClient
import de.stefanbissell.starcruiser.ScanSelectedShip
import de.stefanbissell.starcruiser.SetCoolant
import de.stefanbissell.starcruiser.SetMainScreenView
import de.stefanbissell.starcruiser.SetPower
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.SolveRepairGame
import de.stefanbissell.starcruiser.SolveScanGame
import de.stefanbissell.starcruiser.SpawnShip
import de.stefanbissell.starcruiser.StartJump
import de.stefanbissell.starcruiser.StartRepair
import de.stefanbissell.starcruiser.TogglePause
import de.stefanbissell.starcruiser.ToggleShieldsUp
import de.stefanbissell.starcruiser.Uuid
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

data class ClientId(private val id: String) {

    companion object {
        fun random() = ClientId(Uuid().toString())
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
            when (val command = parseCommandSafely(frame)) {
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
                is Command.CommandMapSelectWaypoint -> gameStateActor.send(
                    MapSelectWaypoint(id, command.index)
                )
                is Command.CommandMapClearSelection -> gameStateActor.send(
                    MapClearSelection(id)
                )
                is Command.CommandMapSelectShip -> gameStateActor.send(
                    MapSelectShip(id, command.targetId)
                )
                is Command.CommandAddWaypoint -> gameStateActor.send(
                    AddWaypoint(id, command.position)
                )
                is Command.CommandDeleteSelectedWaypoint -> gameStateActor.send(
                    DeleteSelectedWaypoint(id)
                )
                is Command.CommandScanSelectedShip -> gameStateActor.send(
                    ScanSelectedShip(id)
                )
                is Command.CommandAbortScan -> gameStateActor.send(
                    AbortScan(id)
                )
                is Command.CommandSolveScanGame -> gameStateActor.send(
                    SolveScanGame(id, command.dimension, command.value)
                )
                is Command.CommandLockTarget -> gameStateActor.send(
                    LockTarget(id, command.targetId)
                )
                is Command.CommandToggleShieldsUp -> gameStateActor.send(
                    ToggleShieldsUp(id)
                )
                is Command.CommandStartRepair -> gameStateActor.send(
                    StartRepair(id, command.systemType)
                )
                is Command.CommandAbortRepair -> gameStateActor.send(
                    AbortRepair(id)
                )
                is Command.CommandSolveRepairGame -> gameStateActor.send(
                    SolveRepairGame(id, command.column, command.row)
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

    private fun parseCommandSafely(frame: Frame): Command? {
        val input = String(frame.data)
        return try {
            Command.parse(input)
        } catch (ex: Exception) {
            null
        }
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
