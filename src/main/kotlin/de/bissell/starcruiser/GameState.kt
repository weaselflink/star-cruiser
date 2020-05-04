package de.bissell.starcruiser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.math.BigDecimal
import java.util.Random
import java.util.UUID

sealed class GameStateChange

object Update: GameStateChange()
object TogglePause : GameStateChange()
class NewGameClient(val clientId: UUID): GameStateChange()
class GameClientDisconnected(val clientId: UUID): GameStateChange()
class ChangeThrottle(val clientId: UUID, val diff: BigDecimal): GameStateChange()
class ChangeRudder(val clientId: UUID, val diff: BigDecimal): GameStateChange()
class GetGameStateMessage(val clientId: UUID, val response: CompletableDeferred<GameStateMessage>) : GameStateChange()

@ObsoleteCoroutinesApi
fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
    val gameState = GameState()
    for (change in channel) {
        when (change) {
            is Update -> gameState.update()
            is NewGameClient -> gameState.createShip(change.clientId)
            is GameClientDisconnected -> gameState.deleteShip(change.clientId)
            is TogglePause -> gameState.togglePaused()
            is ChangeThrottle -> gameState.ships[change.clientId]!!.changeThrottle(change.diff)
            is ChangeRudder -> gameState.ships[change.clientId]!!.changeRudder(change.diff)
            is GetGameStateMessage -> change.response.complete(gameState.toMessage(change.clientId))
        }
    }
}

class GameState {

    private var time = GameTime()
    private var paused = true
    val ships = mutableMapOf<UUID, Ship>()

    fun toMessage(clientId: UUID): GameStateMessage {
        val clientShip = ships[clientId]!!
        return GameStateMessage(
            paused = paused,
            ship = clientShip.toMessage(),
            contacts = ships
                .filter { it.key != clientId }
                .map { it.value }
                .map { it.toContactMessage(clientShip) }
        )
    }

    fun createShip(clientId: UUID) {
        ships[clientId] = Ship(
            position = Vector2(
                x = BigDecimal(Random().nextInt(200) - 100),
                y = BigDecimal(Random().nextInt(200) - 100)
            )
        )
    }

    fun deleteShip(clientId: UUID) {
        ships.remove(clientId)
    }

    fun togglePaused() {
        paused = !paused
    }

    fun update() {
        if (paused) return

        time = time.update()

        ships.forEach { it.value.update(time) }
    }
}

data class GameTime(
    val current: BigDecimal = BigDecimal.ZERO,
    val delta: BigDecimal = "0.02".toBigDecimal()
) {

    fun update() = GameTime(current + delta, delta)
}

class Ship(
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

    fun toMessage() =
        ShipMessage(
            speed = speed,
            position = position,
            rotation = rotation,
            heading = rotation.toHeading(),
            velocity = speed.length(),
            throttle = throttle,
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
