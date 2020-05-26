package de.bissell.starcruiser

import de.bissell.starcruiser.ClientState.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

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

@ObsoleteCoroutinesApi
fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
    val gameState = GameState()
    for (change in channel) {
        when (change) {
            is Update -> gameState.update()
            is NewGameClient -> gameState.clientConnected(change.clientId)
            is GameClientDisconnected -> gameState.clientDisconnected(change.clientId)
            is TogglePause -> gameState.togglePaused()
            is SpawnShip -> gameState.spawnShip()
            is JoinShip -> gameState.joinShip(change.clientId, change.shipId, change.station)
            is ChangeStation -> gameState.changeStation(change.clientId, change.station)
            is ExitShip -> gameState.exitShip(change.clientId)
            is ChangeThrottle -> gameState.changeThrottle(change.clientId, change.value)
            is ChangeRudder -> gameState.changeRudder(change.clientId, change.value)
            is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
            is AddWaypoint -> gameState.addWaypoint(change.clientId, change.position)
        }
    }
}

class GameState {

    private var time = GameTime()
    private val ships = mutableMapOf<UUID, Ship>()
    private val clients = mutableMapOf<UUID, Client>()

    fun toMessage(clientId: UUID): SnapshotMessage {
        val clientShip = clientShip(clientId)
        val client = clients[clientId]!!
        return when (client.state) {
            ShipSelection -> SnapshotMessage.ShipSelection(
                playerShips = ships.values.map(Ship::toPlayerShipMessage)
            )
            Helm -> SnapshotMessage.Helm(
                ship = clientShip!!.toMessage(),
                contacts = getContacts(clientShip, client)
            )
            Navigation -> SnapshotMessage.Navigation(
                ship = clientShip!!.toMessage()
            )
            MainScreen -> SnapshotMessage.MainScreen(
                ship = clientShip!!.toMessage(),
                contacts = getContacts(clientShip, client)
            )
        }
    }

    fun clientConnected(clientId: UUID) {
        clients[clientId] = Client(clientId)
    }

    fun joinShip(clientId: UUID, shipId: UUID, station: Station) {
        clients[clientId]?.also { client ->
            client.state = when (station) {
                Station.Helm -> Helm
                Station.Navigation -> Navigation
                Station.MainScreen -> MainScreen
            }
            client.shipId = shipId
        }
    }

    fun changeStation(clientId: UUID, station: Station) {
        clients[clientId]?.also { client ->
            if (client.shipId != null) {
                client.state = when (station) {
                    Station.Helm -> Helm
                    Station.Navigation -> Navigation
                    Station.MainScreen -> MainScreen
                }
            }
        }
    }

    fun exitShip(clientId: UUID) {
        clients[clientId]?.also {
            it.state = ShipSelection
            it.shipId = null
        }
    }

    fun spawnShip(): UUID {
        return Ship(
            position = Vector2.random(300.0),
            throttle = 100,
            rudder = 30,
            waypoints = mutableListOf(
                Waypoint(1, Vector2.random(1000.0, 500.0)),
                Waypoint(2, Vector2.random(1000.0, 500.0))
            )
        ).also {
            ships[it.id] = it
        }.id
    }

    fun clientDisconnected(clientId: UUID) {
        clients.remove(clientId)
    }

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update()

        ships.forEach { it.value.update(time) }
    }

    fun changeThrottle(clientId: UUID, value: Int) {
        clientShip(clientId)?.changeThrottle(value)
    }

    fun changeRudder(clientId: UUID, value: Int) {
        clientShip(clientId)?.changeRudder(value)
    }

    fun addWaypoint(clientId: UUID, position: Vector2) {
        clientShip(clientId)?.addWaypoint(position)
    }

    private fun clientShip(clientId: UUID): Ship? =
        clients[clientId]?.let { ships[it.shipId] }

    private fun getContacts(clientShip: Ship, client: Client): List<ContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toContactMessage(clientShip) }
            .filter {
                client.state != Helm || it.relativePosition.length() < clientShip.shortRangeScopeRange * 1.1
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
    var shipId: UUID? = null
)

enum class ClientState {
    ShipSelection,
    Helm,
    Navigation,
    MainScreen
}

class Ship(
    val id: UUID = UUID.randomUUID(),
    val shortRangeScopeRange: Double = 400.0,
    private val designation: String = randomShipName(),
    private val shipClass: String = "Infector",
    var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    private var rotation: Double = 90.0.toRadians(),
    private var throttle: Int = 0,
    private var rudder: Int = 0,
    private val waypoints: MutableList<Waypoint> = mutableListOf()
) {

    private var thrust = 0.0

    private val history = mutableListOf<Pair<Double, Vector2>>()

    private val thrustFactor = 0.2
    private val rudderFactor = 10.0

    fun update(time: GameTime) {
        updateThrust(time)
        updateRotation(time)

        speed = Vector2(thrust * thrustFactor, 0.0).rotate(rotation)
        position = (position + speed * time.delta)

        updateHistory(time)
    }

    private fun updateThrust(time: GameTime) {
        val diff = if (throttle > thrust) 10 else if (throttle < thrust) -10 else 0
        thrust += diff * time.delta
    }

    private fun updateRotation(time: GameTime) {
        val diff = -(rudder.toDouble().toRadians() * 0.01 * rudderFactor * PI)
        rotation = (rotation + diff * time.delta)
        if (rotation >= PI * 2) {
            rotation %= (PI * 2)
        }
        if (rotation < 0.0) {
            rotation = PI * 2 + rotation % (PI * 2)
        }
    }

    private fun updateHistory(time: GameTime) {
        if (history.isEmpty()) {
            history.add(Pair(time.current, position))
        } else {
            if (abs(history.last().first - time.current) > 1.0) {
                history.add(Pair(time.current, position))
            }
            if (history.size > 10) {
                history.removeAt(0)
            }
        }
    }

    fun changeThrottle(value: Int) {
        throttle = value.clip(-100, 100)
    }

    fun changeRudder(value: Int) {
        rudder = value.clip(-100, 100)
    }

    fun addWaypoint(position: Vector2) {
        (1..waypoints.size * 2).firstOrNull {
            waypoints.none { waypoint -> waypoint.index == it }
        }?.also {
            waypoints += Waypoint(it, position)
        }
    }

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id.toString(),
            name = designation,
            shipClass = shipClass
        )

    fun toMessage() =
        ShipMessage(
            id = id.toString(),
            designation = designation,
            shipClass = shipClass,
            speed = speed,
            position = position,
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            throttle = throttle,
            thrust = thrust,
            rudder = rudder,
            history = history.map { it.first to it.second },
            shortRangeScopeRange = shortRangeScopeRange,
            waypoints = waypoints.map { it.toWaypointMessage(this) }
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id.toString(),
            designation = designation,
            speed = speed,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            history = history.map { it.first to it.second }
        )
}

data class Waypoint(
    val index: Int,
    val position: Vector2
) {

    fun toWaypointMessage(relativeTo: Ship) =
        WaypointMessage(
            name = "WP$index",
            position = position,
            relativePosition = (position - relativeTo.position)
        )
}
