package de.bissell.starcruiser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random
import java.util.UUID

sealed class GameStateChange

object Update : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
class JoinShip(val clientId: UUID, val shipId: UUID) : GameStateChange()
class NewGameClient(val clientId: UUID) : GameStateChange()
class GameClientDisconnected(val clientId: UUID) : GameStateChange()
class ChangeThrottle(val clientId: UUID, val diff: BigDecimal) : GameStateChange()
class ChangeRudder(val clientId: UUID, val diff: BigDecimal) : GameStateChange()
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
            is ChangeThrottle -> gameState.changeThrottle(change.clientId, change.diff)
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
        val clientShip = clientShip(clientId)!!
        return GameStateSnapshot(
            paused = paused,
            playerShips = ships.values.map(Ship::toPlayerShipMessage),
            ship = clientShip.toMessage(),
            contacts = ships
                .filter { it.key != clientShip.id }
                .map { it.value }
                .map { it.toContactMessage(clientShip) }
        )
    }

    fun joinShip(clientId: UUID, shipId: UUID) {
        clientShipMapping[clientId] = shipId
    }

    fun spawnShip(): UUID {
        return Ship(
            position = Vector2(
                x = BigDecimal(Random().nextInt(200) - 100),
                y = BigDecimal(Random().nextInt(200) - 100)
            )
        ).also {
            ships[it.id] = it
        }.id
    }

    fun spawnShip(clientId: UUID) {
        spawnShip().also { shipId ->
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

    fun changeThrottle(clientId: UUID, diff: BigDecimal) {
        clientShip(clientId)?.changeThrottle(diff)
    }

    fun changeRudder(clientId: UUID, diff: BigDecimal) {
        clientShip(clientId)?.changeRudder(diff)
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
    val name: String = randomShipName(),
    private var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    private var rotation: BigDecimal = 90.toBigDecimal().toRadians()
) {

    private var throttle = BigDecimal.ZERO
    private var thrust = BigDecimal.ZERO
    private var rudder = BigDecimal.ZERO

    private val history = mutableListOf<Pair<BigDecimal, Vector2>>()

    private val thrustFactor = BigDecimal("0.2")
    private val rudderFactor = BigDecimal("0.2")

    fun update(time: GameTime) {
        updateThrust(time)
        updateRotation(time)

        speed = Vector2(thrust * thrustFactor, BigDecimal.ZERO).rotate(rotation).setScale(9)
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

    fun changeThrottle(diff: BigDecimal) {
        throttle = (throttle + diff).clip(-100, 100)
    }

    fun changeRudder(diff: BigDecimal) {
        rudder = (rudder + diff).clip(-100, 100)
    }

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id,
            name = name
        )

    fun toMessage() =
        ShipMessage(
            id = id,
            name = name,
            speed = speed,
            position = position,
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length().setScale(2, RoundingMode.HALF_EVEN),
            throttle = throttle,
            thrust = thrust,
            rudder = rudder,
            history = mutableListOf<Pair<BigDecimal, Vector2>>().apply { addAll(history) }
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            speed = speed,
            position = position,
            relativePosition = position - relativeTo.position,
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            history = mutableListOf<Pair<BigDecimal, Vector2>>().apply { addAll(history) }
        )
}
