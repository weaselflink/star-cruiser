@file:UseSerializers(BigDecimalSerializer::class)

package de.bissell.starcruiser

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.json
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.Duration
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ObsoleteCoroutinesApi
@UnstableDefault
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val gameStateActor = gameStateActor()

    GlobalScope.launch {
        while (isActive) {
            gameStateActor.send(Update)
            delay(20)
        }
    }

    install(Authentication) {
        basic("myBasicAuth") {
            realm = "Star Cruiser"
            validate { if (it.name == "test" && it.password == "password") UserIdPrincipal(it.name) else null }
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        json(
            DefaultJsonConfiguration.copy(
                prettyPrint = true
            )
        )
    }

    routing {
        webUi()

        webSocket("/ws/client") {
            GameClient(
                gameStateActor = gameStateActor,
                outgoing = outgoing,
                incoming = incoming
            ).start()
        }
    }
}

private suspend fun SendChannel<Frame>.sendText(value: String) = send(Frame.Text(value))

class GameClient(
    private val id: UUID = UUID.randomUUID(),
    private val gameStateActor: SendChannel<GameStateCommand>,
    private val outgoing: SendChannel<Frame>,
    private val incoming: ReceiveChannel<Frame>
) {

    @UnstableDefault
    suspend fun start() {
        gameStateActor.send(NewGameClient(id))

        GlobalScope.launch {
            while (isActive) {
                val response = CompletableDeferred<GameStateMessage>()
                gameStateActor.send(GetGameStateMessage(id, response))
                outgoing.sendText(response.await().toJson())
                delay(200)
            }
        }

        for (frame in incoming) {
            when (String(frame.data)) {
                "KeyP" -> gameStateActor.send(Pause(id))

                "KeyW" -> gameStateActor.send(ChangeThrottle(id, BigDecimal(10)))
                "KeyS" -> gameStateActor.send(ChangeThrottle(id, BigDecimal(-10)))
                "KeyA" -> gameStateActor.send(ChangeRudder(id, BigDecimal(-10)))
                "KeyD" -> gameStateActor.send(ChangeRudder(id, BigDecimal(10)))
            }
        }
    }
}

@Serializable
data class GameStateMessage(
    val paused: Boolean,
    val ship: ShipMessage,
    val contacts: List<ShipMessage>
) {
    @UnstableDefault
    fun toJson(): String = Json.stringify(serializer(), this)
}

@Serializable
data class ShipMessage(
    val position: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val throttle: BigDecimal,
    val rudder: BigDecimal,
    val history: List<Pair<BigDecimal, Vector2>>
)

sealed class GameStateCommand

sealed class ClientGameStateCommand(
    val clientId: UUID
): GameStateCommand()

object Update: GameStateCommand()
class NewGameClient(clientId: UUID): ClientGameStateCommand(clientId)
class Pause(clientId: UUID): ClientGameStateCommand(clientId)
class ChangeThrottle(clientId: UUID, val diff: BigDecimal): ClientGameStateCommand(clientId)
class ChangeRudder(clientId: UUID, val diff: BigDecimal): ClientGameStateCommand(clientId)
class GetGameStateMessage(clientId: UUID, val response: CompletableDeferred<GameStateMessage>) : ClientGameStateCommand(clientId)

@ObsoleteCoroutinesApi
fun CoroutineScope.gameStateActor() = actor<GameStateCommand> {
    val gameState = GameState()
    for (command in channel) {
        when (command) {
            is Update -> gameState.update()
            is NewGameClient -> gameState.createShip(command.clientId)
            is Pause -> gameState.togglePaused()
            is ChangeThrottle -> gameState.ships[command.clientId]!!.changeThrottle(command.diff)
            is ChangeRudder -> gameState.ships[command.clientId]!!.changeRudder(command.diff)
            is GetGameStateMessage -> command.response.complete(gameState.toMessage(command.clientId))
        }
    }
}

class GameState {

    private var time = GameTime()
    private var paused = true
    val ships = mutableMapOf<UUID, Ship>()

    fun toMessage(clientId: UUID) =
        GameStateMessage(
            paused = paused,
            ship = ships[clientId]!!.toMessage(),
            contacts = ships
                .filter { it.key != clientId }
                .map { it.value }
                .map { it.toMessage() }
        )

    fun createShip(clientId: UUID) {
        ships[clientId] = Ship(
            position = Vector2(
                x = BigDecimal(Random().nextInt(200) - 100),
                y = BigDecimal(Random().nextInt(200) - 100)
            )
        )
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
    val current: BigDecimal = ZERO,
    val delta: BigDecimal = "0.02".toBigDecimal()
) {

    fun update() = GameTime(current + delta, delta)
}

class Ship(
    private var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    private var rotation: BigDecimal = 90.toBigDecimal().toRadians()
) {

    private var throttle = ZERO
    private var thrust = ZERO
    private var rudder = ZERO

    private val history = mutableListOf<Pair<BigDecimal, Vector2>>()

    private val thrustFactor = BigDecimal("0.2")
    private val rudderFactor = BigDecimal("0.2")

    fun update(time: GameTime) {
        updateThrust(time)
        updateRotation(time)

        speed = Vector2(thrust * thrustFactor, ZERO).rotate(rotation).setScale(9)
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
        if (rotation < ZERO) {
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
}
