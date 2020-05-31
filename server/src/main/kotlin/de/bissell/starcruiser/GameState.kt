package de.bissell.starcruiser

import de.bissell.starcruiser.ClientState.InShip
import de.bissell.starcruiser.ClientState.ShipSelection
import de.bissell.starcruiser.Station.*
import de.bissell.starcruiser.ships.Ship
import de.bissell.starcruiser.ships.ShipTemplate
import de.bissell.starcruiser.ships.Waypoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit
import java.util.*

sealed class GameStateChange

object Update : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
class JoinShip(val clientId: UUID, val shipId: UUID, val station: Station) : GameStateChange()
class ChangeStation(val clientId: UUID, val station: Station) : GameStateChange()
class ExitShip(val clientId: UUID) : GameStateChange()
class NewGameClient(val clientId: UUID) : GameStateChange()
class GameClientDisconnected(val clientId: UUID) : GameStateChange()
class ChangeThrottle(val clientId: UUID, val value: Int) : GameStateChange()
class ChangeRudder(val clientId: UUID, val value: Int) : GameStateChange()
class GetGameStateSnapshot(val clientId: UUID, val response: CompletableDeferred<SnapshotMessage>) : GameStateChange()
class AddWaypoint(val clientId: UUID, val position: Vector2) : GameStateChange()
class DeleteWaypoint(val clientId: UUID, val position: Vector2) : GameStateChange()

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
            is DeleteWaypoint -> gameState.deleteWaypoint(change.clientId, change.position)
        }
    }
}

class GameState {

    private var time = GameTime()
    private val ships = mutableMapOf<UUID, Ship>()
    private val clients = mutableMapOf<UUID, Client>()

    private val physicsEngine = PhysicsEngine()

    fun toMessage(clientId: UUID): SnapshotMessage {
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
                Navigation -> SnapshotMessage.Navigation(
                    ship = clientShip!!.toMessage(),
                    contacts = getContacts(clientShip, client)
                )
                MainScreen -> SnapshotMessage.MainScreen(
                    ship = clientShip!!.toMessage(),
                    contacts = getContacts(clientShip, client)
                )
            }
        }
    }

    fun clientConnected(clientId: UUID) {
        getClient(clientId)
    }

    fun clientDisconnected(clientId: UUID) {
        clients.remove(clientId)
    }

    fun joinShip(clientId: UUID, shipId: UUID, station: Station) {
        getClient(clientId).joinShip(shipId, station)
    }

    fun changeStation(clientId: UUID, station: Station) {
        getClient(clientId).changeStation(station)
    }

    fun exitShip(clientId: UUID) {
        getClient(clientId).exitShip()
    }

    fun spawnShip(): UUID =
        Ship(
            template = ShipTemplate(),
            position = Vector2.random(300.0),
            throttle = 0,
            rudder = 0,
            waypoints = mutableListOf(
                Waypoint(1, Vector2.random(1000.0, 500.0)),
                Waypoint(2, Vector2.random(1000.0, 500.0))
            )
        ).also {
            ships[it.id] = it
            physicsEngine.addShip(it)
        }.id

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update()

        physicsEngine.step(time)
        ships.forEach { it.value.update(time, physicsEngine) }
    }

    fun changeThrottle(clientId: UUID, value: Int) {
        getClientShip(clientId)?.changeThrottle(value)
    }

    fun changeRudder(clientId: UUID, value: Int) {
        getClientShip(clientId)?.changeRudder(value)
    }

    fun addWaypoint(clientId: UUID, position: Vector2) {
        getClientShip(clientId)?.addWaypoint(position)
    }

    fun deleteWaypoint(clientId: UUID, position: Vector2) {
        getClientShip(clientId)?.deleteWaypoint(position)
    }

    private fun getClient(clientId: UUID) =
        clients.computeIfAbsent(clientId) { Client(clientId) }

    private fun getClientShip(clientId: UUID): Ship? =
        getClient(clientId).let { ships[it.shipId] }

    private fun getContacts(clientShip: Ship, client: Client): List<ContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toContactMessage(clientShip) }
            .filter {
                client.station != Helm || it.relativePosition.length() < clientShip.shortRangeScopeRange * 1.1
            }
    }

    private fun getScopeContacts(clientShip: Ship): List<ScopeContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toScopeContactMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.shortRangeScopeRange * 1.1
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

    fun update() {
        val now = now()
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
    val id: UUID,
    var state: ClientState = ShipSelection,
    var shipId: UUID? = null,
    var station: Station? = null
) {

    fun joinShip(shipId: UUID, station: Station) {
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
