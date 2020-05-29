package de.bissell.starcruiser

import de.bissell.starcruiser.ClientState.*
import de.bissell.starcruiser.Station.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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
        }
    }
}

class GameState {

    private var time = GameTime()
    private val ships = mutableMapOf<UUID, Ship>()
    private val clients = mutableMapOf<UUID, Client>()

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

    fun spawnShip(): UUID {
        return Ship(
            template = ShipTemplate(),
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

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update()

        ships.forEach { it.value.update(time) }
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

class Ship(
    val id: UUID = UUID.randomUUID(),
    val template: ShipTemplate,
    val shortRangeScopeRange: Double = 400.0,
    private val designation: String = randomShipName(),
    var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    private var rotation: Double = 90.0.toRadians(),
    private var throttle: Int = 0,
    private var rudder: Int = 0,
    private val waypoints: MutableList<Waypoint> = mutableListOf()
) {

    private var thrust = 0.0

    private val history = mutableListOf<Pair<Double, Vector2>>()

    fun update(time: GameTime) {
        updateThrust(time)
        updateRotation(time)

        val effectiveThrust = if (thrust < 0) {
            thrust * template.reverseThrustFactor
        } else {
            thrust * template.aheadThrustFactor
        }
        speed = Vector2(effectiveThrust, 0.0).rotate(rotation)
        position = (position + speed * time.delta)

        updateHistory(time)
    }

    private fun updateThrust(time: GameTime) {
        val responsiveness = template.throttleResponsiveness
        val diff = if (throttle > thrust) responsiveness else if (throttle < thrust) -responsiveness else 0.0
        thrust = (thrust + diff * time.delta).clamp(-100.0, 100.0)
    }

    private fun updateRotation(time: GameTime) {
        val diff = -(rudder.toDouble().toRadians() * 0.01 * template.rudderFactor * PI)
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
        throttle = value.clamp(-100, 100)
    }

    fun changeRudder(value: Int) {
        rudder = value.clamp(-100, 100)
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
            shipClass = template.className
        )

    fun toMessage() =
        ShipMessage(
            id = id.toString(),
            designation = designation,
            shipClass = template.className,
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
