package de.bissell.starcruiser

import de.bissell.starcruiser.ClientState.Helm
import de.bissell.starcruiser.ClientState.ShipSelection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.util.Random
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs

sealed class GameStateChange

object Update : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
class JoinShip(val clientId: UUID, val shipId: UUID) : GameStateChange()
class ExitShip(val clientId: UUID) : GameStateChange()
class NewGameClient(val clientId: UUID) : GameStateChange()
class GameClientDisconnected(val clientId: UUID) : GameStateChange()
class ChangeThrottle(val clientId: UUID, val value: Int) : GameStateChange()
class ChangeRudder(val clientId: UUID, val value: Int) : GameStateChange()
class GetGameStateSnapshot(val clientId: UUID, val response: CompletableDeferred<GameStateSnapshot>) : GameStateChange()

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
            is JoinShip -> gameState.joinShip(change.clientId, change.shipId)
            is ExitShip -> gameState.exitShip(change.clientId)
            is ChangeThrottle -> gameState.changeThrottle(change.clientId, change.value)
            is ChangeRudder -> gameState.changeRudder(change.clientId, change.value)
            is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
        }
    }
}

class GameState {

    private var time = GameTime()
    private var paused = false
    private val ships = mutableMapOf<UUID, Ship>()
    private val clients = mutableMapOf<UUID, Client>()

    fun toMessage(clientId: UUID): GameStateSnapshot {
        val clientShip = clientShip(clientId)
        val client = clients[clientId]!!
        return GameStateSnapshot(
            clientState = client.state,
            paused = paused,
            playerShips = ships.values.map(Ship::toPlayerShipMessage),
            ship = clientShip?.toMessage(),
            contacts = if (clientShip == null) {
                emptyList()
            } else {
                ships
                    .filter { it.key != clientShip.id }
                    .map { it.value }
                    .map { it.toContactMessage(clientShip) }
                    .filter { it.relativePosition.length() < clientShip.shortRangeScopeRange * 1.1 }
            }
        )
    }

    fun clientConnected(clientId: UUID) {
        clients[clientId] = Client(clientId)
    }

    fun joinShip(clientId: UUID, shipId: UUID) {
        clients[clientId]?.also {
            it.state = Helm
            it.shipId = shipId
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
            position = Vector2(
                x = Random().nextInt(500) - 250.0,
                y = Random().nextInt(500) - 250.0
            ),
            throttle = 100,
            rudder = 30
        ).also {
            ships[it.id] = it
        }.id
    }

    fun clientDisconnected(clientId: UUID) {
        clients.remove(clientId)
    }

    fun togglePaused() {
        paused = !paused
    }

    fun update() {
        if (paused) return

        time = time.update()

        ships.forEach { it.value.update(time) }
    }

    fun changeThrottle(clientId: UUID, value: Int) {
        clientShip(clientId)?.changeThrottle(value)
    }

    fun changeRudder(clientId: UUID, value: Int) {
        clientShip(clientId)?.changeRudder(value)
    }

    private fun clientShip(clientId: UUID): Ship? =
        clients[clientId]?.let { ships[it.shipId] }
}

data class GameTime(
    val current: Double = 0.0,
    val delta: Double = 0.02
) {

    fun update() = GameTime(current + delta, delta)
}

data class Client(
    val id: UUID,
    var state: ClientState = ShipSelection,
    var shipId: UUID? = null
)

class Ship(
    val id: UUID = UUID.randomUUID(),
    val shortRangeScopeRange: Double = 400.0,
    private val designation: String = randomShipName(),
    private val shipClass: String = "Infector",
    private var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    private var rotation: Double = 90.0.toRadians(),
    private var throttle: Int = 0,
    private var rudder: Int = 0
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
            shortRangeScopeRange = shortRangeScopeRange
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
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
