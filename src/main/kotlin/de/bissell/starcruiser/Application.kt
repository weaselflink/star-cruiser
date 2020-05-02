@file:UseSerializers(BigDecimalSerializer::class)

package de.bissell.starcruiser

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.html.respondHtml
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.json
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ObsoleteCoroutinesApi
@UnstableDefault
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val gameStateActor = gameStateActor()

    GlobalScope.launch {
        while (true) {
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
        static("static") {
            files("js")
        }

        get("/") {
            call.respondHtml {
                head {
                    script {
                        type = ScriptType.textJavaScript
                        src = "/static/bla.js"
                    }
                }
                body {
                    h1 { +"Star Cruiser" }
                    p {
                        span {
                            id = "conn"
                            +"disconnected"
                        }
                    }
                    canvas {
                        id = "canvas"
                        width = "800px"
                        height = "800px"
                    }
                    p {
                        +"Game state: "
                        span {
                            id = "pos"
                            +"unknown"
                        }
                    }
                }
            }
        }

        authenticate("myBasicAuth") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }

        webSocket("/ws/updates") {
            while (isActive) {
                val response = CompletableDeferred<GameStateMessage>()
                gameStateActor.send(GetGameStateMessage(response))
                outgoing.sendText(response.await().toJson())
                delay(100)
            }
        }

        webSocket("/ws/command") {
            for (frame in incoming) {
                when (String(frame.data)) {
                    "KeyP" -> gameStateActor.send(Pause)

                    "KeyW" -> gameStateActor.send(ChangeThrottle(BigDecimal(10)))
                    "KeyS" -> gameStateActor.send(ChangeThrottle(BigDecimal(-10)))
                    "KeyA" -> gameStateActor.send(ChangeRudder(BigDecimal(-10)))
                    "KeyD" -> gameStateActor.send(ChangeRudder(BigDecimal(10)))
                }
            }
        }
    }
}

private suspend fun SendChannel<Frame>.sendText(value: String) = send(Frame.Text(value))

@Serializable
data class GameStateMessage(
    val paused: Boolean,
    val ships: List<ShipMessage>
) {
    @UnstableDefault
    fun toJson(): String = Json.stringify(serializer(), this)
}

@Serializable
data class ShipMessage(
    val position: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val throttle: BigDecimal,
    val rudder: BigDecimal,
    val history: List<Pair<BigDecimal, Vector2>>
)

sealed class GameStateCommand

object Update: GameStateCommand()
object Pause: GameStateCommand()
class ChangeThrottle(val diff: BigDecimal): GameStateCommand()
class ChangeRudder(val diff: BigDecimal): GameStateCommand()
class GetGameStateMessage(val response: CompletableDeferred<GameStateMessage>) : GameStateCommand()

@ObsoleteCoroutinesApi
fun CoroutineScope.gameStateActor() = actor<GameStateCommand> {
    val gameState = GameState()
    for (command in channel) {
        when (command) {
            is Update -> gameState.update()
            is Pause -> gameState.paused = !gameState.paused
            is ChangeThrottle -> gameState.ships.first().changeThrottle(command.diff)
            is ChangeRudder -> gameState.ships.first().changeRudder(command.diff)
            is GetGameStateMessage -> command.response.complete(gameState.toMessage())
        }
    }
}

class GameState {

    var time = GameTime()
    var paused = true
    val ships = mutableListOf(Ship())

    fun toMessage() =
        GameStateMessage(
            paused = paused,
            ships = ships.map { it.toMessage() }
        )

    fun update() {
        if (paused) return

        time = time.update()

        ships.forEach { it.update(time) }
    }
}

data class GameTime(
    val current: BigDecimal = BigDecimal(0),
    val delta: BigDecimal = BigDecimal("0.02")
) {

    fun update() = GameTime(current + delta, delta)
}

class Ship {
    private var position = Vector2()
    private var speed = Vector2()
    private var rotation = BigDecimal(90).toRadians()

    private var throttle = BigDecimal.ZERO
    private var thrust = BigDecimal.ZERO
    private var rudder = BigDecimal.ZERO

    private val history = mutableListOf<Pair<BigDecimal, Vector2>>()

    fun update(time: GameTime) {
        val diff = if (throttle > thrust) 10 else if (throttle < thrust) -10 else 0
        thrust += diff.toBigDecimal() * time.delta

        rotation = (rotation + (rudder.toRadians() * PI * time.delta)).setScale(9, RoundingMode.FLOOR)

        speed = Vector2(thrust, BigDecimal.ZERO).rotate(rotation).setScale(9)
        position = (position + speed * time.delta).setScale(9)

        if (history.isEmpty()) {
            history.add(Pair(time.current, position))
        } else {
            if ((history.last().first - time.current).abs() > BigDecimal(1)) {
                history.add(Pair(time.current, position))
            }
            if (history.size > 10) {
                history.removeAt(0)
            }
        }
    }

    fun changeThrottle(diff: BigDecimal) {
        throttle = (throttle + diff).constrain(BigDecimal(-100), BigDecimal(100))
    }

    fun changeRudder(diff: BigDecimal) {
        rudder = (rudder + diff).constrain(BigDecimal(-100), BigDecimal(100))
    }

    fun toMessage() =
        ShipMessage(
            speed = speed,
            position = position,
            rotation = rotation,
            throttle = throttle,
            rudder = rudder,
            history = mutableListOf<Pair<BigDecimal, Vector2>>().apply { addAll(history) }
        )
}
