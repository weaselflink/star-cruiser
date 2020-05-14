package de.bissell.starcruiser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.util.*
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
class ChangeRudder(val clientId: UUID, val diff: Int) : GameStateChange()
class GetGameStateSnapshot(val clientId: UUID, val response: CompletableDeferred<GameStateSnapshot>) : GameStateChange()

@ObsoleteCoroutinesApi
fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
    val gameState = GameState()
    for (change in channel) {
        when (change) {
            is Update -> gameState.update()
            is NewGameClient -> {}
            is GameClientDisconnected -> gameState.deleteShip(change.clientId)
            is TogglePause -> gameState.togglePaused()
            is SpawnShip -> gameState.spawnShip()
            is JoinShip -> gameState.joinShip(change.clientId, change.shipId)
            is ExitShip -> gameState.exitShip(change.clientId)
            is ChangeThrottle -> gameState.changeThrottle(change.clientId, change.value)
            is ChangeRudder -> gameState.changeRudder(change.clientId, change.diff)
            is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
        }
    }
}

class GameState {

    private var time = GameTime()
    private var paused = false
    private val ships = mutableMapOf<UUID, Ship>()
    private val clientShipMapping = mutableMapOf<UUID, UUID>()

    fun toMessage(clientId: UUID): GameStateSnapshot {
        val clientShip = clientShip(clientId)
        return GameStateSnapshot(
            paused = paused,
            playerShips = ships.values.map(Ship::toPlayerShipMessage),
            ship = clientShip?.toMessage(),
            contacts = ships
                .filter { it.key != clientShip?.id }
                .map { it.value }
                .map { it.toContactMessage(clientShip) }
        )
    }

    fun joinShip(clientId: UUID, shipId: UUID) {
        clientShipMapping[clientId] = shipId
    }

    fun exitShip(clientId: UUID) {
        clientShipMapping.remove(clientId)
    }

    fun spawnShip(): UUID {
        return Ship(
            position = Vector2(
                x = Random().nextInt(200) - 100.0,
                y = Random().nextInt(200) - 100.0
            ),
            throttle = 100,
            rudder = 30
        ).also {
            ships[it.id] = it
        }.id
    }

    fun deleteShip(clientId: UUID) {
        clientShipMapping[clientId]?.also {
            ships.remove(it)
        }
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

    fun changeRudder(clientId: UUID, diff: Int) {
        clientShip(clientId)?.changeRudder(diff)
    }

    private fun clientShip(clientId: UUID): Ship? =
        clientShipMapping[clientId]?.let {
            ships[it]
        }
}

data class GameTime(
    val current: Double = 0.0,
    val delta: Double = 0.02
) {

    fun update() = GameTime(current + delta, delta)
}

class Ship(
    val id: UUID = UUID.randomUUID(),
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
    private val rudderFactor = 0.2

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
        val diff = -(rudder.toDouble().toRadians() * rudderFactor * PI)
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

    fun changeRudder(diff: Int) {
        rudder = (rudder + diff).clip(-100, 100)
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
            history = history.map { it.first to it.second }
        )

    fun toContactMessage(relativeTo: Ship?) =
        ContactMessage(
            speed = speed,
            position = position,
            relativePosition = (position - (relativeTo?.position ?: Vector2())),
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            history = history.map { it.first to it.second }
        )
}
