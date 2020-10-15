package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.client.ClientId
import kotlinx.coroutines.CompletableDeferred

sealed class GameStateChange

object Restart : GameStateChange()
object Update : GameStateChange()
data class GetGameStateSnapshot(val clientId: ClientId, val response: CompletableDeferred<SnapshotMessage>) : GameStateChange()
object TogglePause : GameStateChange()
object SpawnPlayerShip : GameStateChange()
data class JoinShip(val clientId: ClientId, val objectId: ObjectId, val station: Station) : GameStateChange()
data class ChangeStation(val clientId: ClientId, val station: Station) : GameStateChange()
data class ExitShip(val clientId: ClientId) : GameStateChange()
data class NewGameClient(val clientId: ClientId) : GameStateChange()
data class GameClientDisconnected(val clientId: ClientId) : GameStateChange()
data class SetThrottle(val clientId: ClientId, val value: Int) : GameStateChange()
data class ChangeJumpDistance(val clientId: ClientId, val value: Double) : GameStateChange()
data class StartJump(val clientId: ClientId) : GameStateChange()
data class SetRudder(val clientId: ClientId, val value: Int) : GameStateChange()
data class MapClearSelection(val clientId: ClientId) : GameStateChange()
data class MapSelectWaypoint(val clientId: ClientId, val index: Int) : GameStateChange()
data class MapSelectShip(val clientId: ClientId, val targetId: ObjectId) : GameStateChange()
data class AddWaypoint(val clientId: ClientId, val position: Vector2) : GameStateChange()
data class DeleteSelectedWaypoint(val clientId: ClientId) : GameStateChange()
data class ScanSelectedShip(val clientId: ClientId) : GameStateChange()
data class AbortScan(val clientId: ClientId) : GameStateChange()
data class SolveScanGame(val clientId: ClientId, val dimension: Int, val value: Double) : GameStateChange()
data class LockTarget(val clientId: ClientId, val targetId: ObjectId) : GameStateChange()
data class ToggleShieldsUp(val clientId: ClientId) : GameStateChange()
data class StartRepair(val clientId: ClientId, val systemType: PoweredSystemType) : GameStateChange()
data class AbortRepair(val clientId: ClientId) : GameStateChange()
data class SolveRepairGame(val clientId: ClientId, val column: Int, val row: Int) : GameStateChange()
data class SetPower(val clientId: ClientId, val systemType: PoweredSystemType, val power: Int) : GameStateChange()
data class SetCoolant(val clientId: ClientId, val systemType: PoweredSystemType, val coolant: Double) : GameStateChange()
data class SetMainScreenView(val clientId: ClientId, val mainScreenView: MainScreenView) : GameStateChange()
