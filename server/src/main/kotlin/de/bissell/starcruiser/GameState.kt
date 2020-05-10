package de.bissell.starcruiser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.math.BigDecimal
import java.util.*

sealed class GameStateChange

object Update : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
class JoinShip(val clientId: UUID, val shipId: UUID) : GameStateChange()
class NewGameClient(val clientId: UUID) : GameStateChange()
class GameClientDisconnected(val clientId: UUID) : GameStateChange()
class ChangeThrottle(val clientId: UUID, val value: Long) : GameStateChange()
class ChangeRudder(val clientId: UUID, val diff: Long) : GameStateChange()
class GetGameStateSnapshot(val clientId: UUID, val response: CompletableDeferred<GameStateSnapshot>) : GameStateChange()

@ObsoleteCoroutinesApi
fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
    val gameState = GameState()
    for (change in channel) {
        when (change) {
            is Update -> gameState.update()
            is NewGameClient -> gameState.spawnShip(change.clientId)
            is GameClientDisconnected -> gameState.deleteShip(change.clientId)
            is TogglePause -> gameState.togglePaused()
            is SpawnShip -> gameState.spawnShip()
            is JoinShip -> gameState.joinShip(change.clientId, change.shipId)
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

    fun spawnShip(): UUID {
        return Ship(
            position = BigVector(
                x = BigDecimal(Random().nextInt(200) - 100),
                y = BigDecimal(Random().nextInt(200) - 100)
            ),
            throttle = 100.toBigDecimal(),
            rudder = 30.toBigDecimal()
        ).also {
            ships[it.id] = it
        }.id
    }

    fun spawnShip(clientId: UUID) {
        (1..4).map {
            spawnShip()
        }.first().also { shipId ->
            joinShip(clientId, shipId)
        }
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

    fun changeThrottle(clientId: UUID, value: Long) {
        clientShip(clientId)?.changeThrottle(value.toBigDecimal())
    }

    fun changeRudder(clientId: UUID, diff: Long) {
        clientShip(clientId)?.changeRudder(diff.toBigDecimal())
    }

    private fun clientShip(clientId: UUID): Ship? =
        clientShipMapping[clientId]?.let {
            ships[it]
        }
}

data class GameTime(
    val current: BigDecimal = BigDecimal.ZERO,
    val delta: BigDecimal = "0.02".toBigDecimal()
) {

    fun update() = GameTime(current + delta, delta)
}

class Ship(
    val id: UUID = UUID.randomUUID(),
    private val name: String = randomShipName(),
    private var position: BigVector = BigVector(),
    private var speed: BigVector = BigVector(),
    private var rotation: BigDecimal = 90.toBigDecimal().toRadians(),
    private var throttle: BigDecimal = BigDecimal.ZERO,
    private var rudder: BigDecimal = BigDecimal.ZERO
) {

    private var thrust = BigDecimal.ZERO

    private val history = mutableListOf<Pair<BigDecimal, BigVector>>()

    private val thrustFactor = BigDecimal("0.2")
    private val rudderFactor = BigDecimal("0.2")

    fun update(time: GameTime) {
        updateThrust(time)
        updateRotation(time)

        speed = BigVector(thrust * thrustFactor, BigDecimal.ZERO).rotate(rotation).setScale(9)
        position = (position + speed * time.delta).setScale(9)

        updateHistory(time)
    }

    private fun updateThrust(time: GameTime) {
        val diff = if (throttle > thrust) 10 else if (throttle < thrust) -10 else 0
        thrust += diff.toBigDecimal() * time.delta
    }

    private fun updateRotation(time: GameTime) {
        val diff = (rudder.toRadians() * rudderFactor * PI).negate()
        rotation = (rotation + diff * time.delta)
        if (rotation >= PI * 2) {
            rotation = rotation.remainder(PI * 2)
        }
        if (rotation < BigDecimal.ZERO) {
            rotation = PI * 2 + rotation.remainder(PI * 2)
        }
        rotation = rotation.defaultScale()
    }

    private fun updateHistory(time: GameTime) {
        if (history.isEmpty()) {
            history.add(Pair(time.current, position))
        } else {
            if ((history.last().first - time.current).abs() > 1.toBigDecimal()) {
                history.add(Pair(time.current, position))
            }
            if (history.size > 10) {
                history.removeAt(0)
            }
        }
    }

    fun changeThrottle(value: BigDecimal) {
        throttle = value.clip(-100, 100)
    }

    fun changeRudder(diff: BigDecimal) {
        rudder = (rudder + diff).clip(-100, 100)
    }

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id.toString(),
            name = name
        )

    fun toMessage() =
        ShipMessage(
            id = id.toString(),
            name = name,
            speed = speed.toVector2(),
            position = position.toVector2(),
            rotation = rotation.toDouble(),
            heading = rotation.toHeading().toDouble(),
            velocity = speed.length().toDouble(),
            throttle = throttle.toDouble(),
            thrust = thrust.toDouble(),
            rudder = rudder.toDouble(),
            history = history.map {
                Pair(it.first.toDouble(), it.second.toVector2())
            }
        )

    fun toContactMessage(relativeTo: Ship?) =
        ContactMessage(
            speed = speed.toVector2(),
            position = position.toVector2(),
            relativePosition = (position - (relativeTo?.position ?: BigVector())).toVector2(),
            rotation = rotation.toDouble(),
            heading = rotation.toHeading().toDouble(),
            velocity = speed.length().toDouble(),
            history = history.map {
                Pair(it.first.toDouble(), it.second.toVector2())
            }
        )
}
