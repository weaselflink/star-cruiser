package de.bissell.starcruiser

import de.bissell.starcruiser.ClientState.InShip
import de.bissell.starcruiser.ClientState.ShipSelection
import de.bissell.starcruiser.Station.*
import de.bissell.starcruiser.client.ClientId
import de.bissell.starcruiser.ships.Ship
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit

sealed class GameStateChange

object Update : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
class JoinShip(val clientId: ClientId, val shipId: ShipId, val station: Station) : GameStateChange()
class ChangeStation(val clientId: ClientId, val station: Station) : GameStateChange()
class ExitShip(val clientId: ClientId) : GameStateChange()
class NewGameClient(val clientId: ClientId) : GameStateChange()
class GameClientDisconnected(val clientId: ClientId) : GameStateChange()
class ChangeThrottle(val clientId: ClientId, val value: Int) : GameStateChange()
class ChangeRudder(val clientId: ClientId, val value: Int) : GameStateChange()
class GetGameStateSnapshot(val clientId: ClientId, val response: CompletableDeferred<SnapshotMessage>) : GameStateChange()
class AddWaypoint(val clientId: ClientId, val position: Vector2) : GameStateChange()
class DeleteWaypoint(val clientId: ClientId, val index: Int) : GameStateChange()
class ScanShip(val clientId: ClientId, val targetId: ShipId) : GameStateChange()
class LockTarget(val clientId: ClientId, val targetId: ShipId) : GameStateChange()

class GameState {

    private var time = GameTime()
    private val ships = mutableMapOf<ShipId, Ship>()
    private val clients = mutableMapOf<ClientId, Client>()

    private val physicsEngine = PhysicsEngine()

    fun toMessage(clientId: ClientId): SnapshotMessage {
        val client = getClient(clientId)
        val clientShip = getClientShip(clientId)
        return when (client.state) {
            ShipSelection -> SnapshotMessage.ShipSelection(
                playerShips = ships.values.map(Ship::toPlayerShipMessage)
            )
            InShip -> when (client.station!!) {
                Helm -> SnapshotMessage.Helm(
                    ship = clientShip!!.toMessage(),
                    contacts = getScopeContacts(clientShip)
                )
                Weapons -> SnapshotMessage.Weapons(
                    ship = clientShip!!.toMessage(),
                    contacts = getScopeContacts(clientShip)
                )
                Navigation -> SnapshotMessage.Navigation(
                    ship = clientShip!!.toMessage(),
                    contacts = getContacts(clientShip)
                )
                MainScreen -> SnapshotMessage.MainScreen(
                    ship = clientShip!!.toMessage(),
                    contacts = getContacts(clientShip)
                )
            }
        }
    }

    fun clientConnected(clientId: ClientId) {
        getClient(clientId)
    }

    fun clientDisconnected(clientId: ClientId) {
        clients.remove(clientId)
    }

    fun joinShip(clientId: ClientId, shipId: ShipId, station: Station) {
        getClient(clientId).joinShip(shipId, station)
    }

    fun changeStation(clientId: ClientId, station: Station) {
        getClient(clientId).changeStation(station)
    }

    fun exitShip(clientId: ClientId) {
        getClient(clientId).exitShip()
    }

    fun spawnShip() =
        Ship(
            position = Vector2.random(300)
        ).also {
            it.addWaypoint(Vector2.random(1000, 500))
            it.addWaypoint(Vector2.random(1000, 500))
            ships[it.id] = it
            physicsEngine.addShip(it)
        }.id

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update(now())

        physicsEngine.step(time)
        ships.forEach { it.value.update(time, physicsEngine) { id -> ships[id] } }
    }

    fun changeThrottle(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.changeThrottle(value)
    }

    fun changeRudder(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.changeRudder(value)
    }

    fun addWaypoint(clientId: ClientId, position: Vector2) {
        getClientShip(clientId)?.addWaypoint(position)
    }

    fun deleteWaypoint(clientId: ClientId, index: Int) {
        getClientShip(clientId)?.deleteWaypoint(index)
    }

    fun scanShip(clientId: ClientId, targetId: ShipId) {
        ships[targetId]?.also {
            getClientShip(clientId)?.startScan(targetId)
        }
    }

    fun lockTarget(clientId: ClientId, targetId: ShipId) {
        ships[targetId]?.also {
            getClientShip(clientId)?.lockTarget(targetId)
        }
    }

    private fun getClient(clientId: ClientId) =
        clients.computeIfAbsent(clientId) { Client(clientId) }

    private fun getClientShip(clientId: ClientId): Ship? =
        getClient(clientId).shipId?.let { ships[it] }

    private fun getContacts(clientShip: Ship): List<ContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toContactMessage(clientShip) }
    }

    private fun getScopeContacts(clientShip: Ship): List<ScopeContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toScopeContactMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.template.shortRangeScopeRange * 1.1
            }
    }

    companion object {
        fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
            val gameState = GameState()
            for (change in channel) {
                when (change) {
                    is Update -> gameState.update()
                    is TogglePause -> gameState.togglePaused()
                    is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
                    is NewGameClient -> gameState.clientConnected(change.clientId)
                    is GameClientDisconnected -> gameState.clientDisconnected(change.clientId)
                    is JoinShip -> gameState.joinShip(change.clientId, change.shipId, change.station)
                    is ChangeStation -> gameState.changeStation(change.clientId, change.station)
                    is ExitShip -> gameState.exitShip(change.clientId)
                    is SpawnShip -> gameState.spawnShip()
                    is ChangeThrottle -> gameState.changeThrottle(change.clientId, change.value)
                    is ChangeRudder -> gameState.changeRudder(change.clientId, change.value)
                    is AddWaypoint -> gameState.addWaypoint(change.clientId, change.position)
                    is DeleteWaypoint -> gameState.deleteWaypoint(change.clientId, change.index)
                    is ScanShip -> gameState.scanShip(change.clientId, change.targetId)
                    is LockTarget -> gameState.lockTarget(change.clientId, change.targetId)
                }
            }
        }
    }
}

class GameTime {

    private var lastUpdate: Instant? = null

    var current: Double = 0.0
        private set

    var delta: Double = 0.001
        private set

    var paused: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                lastUpdate = null
            }
        }

    fun update(now: Instant) {
        delta = if (lastUpdate == null) {
            0.001
        } else {
            (lastUpdate!!.until(now, ChronoUnit.MILLIS)) / 1000.0
        }
        current += delta
        lastUpdate = now
    }
}

data class Client(
    val id: ClientId,
    var state: ClientState = ShipSelection,
    var shipId: ShipId? = null,
    var station: Station? = null
) {

    fun joinShip(shipId: ShipId, station: Station) {
        state = InShip
        this.shipId = shipId
        this.station = station
    }

    fun changeStation(station: Station) {
        if (shipId != null) {
            this.station = station
        }
    }

    fun exitShip() {
        state = ShipSelection
        shipId = null
        station = null
    }
}

enum class ClientState {
    ShipSelection,
    InShip
}
