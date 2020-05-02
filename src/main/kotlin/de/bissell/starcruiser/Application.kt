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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@UnstableDefault
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val gameState = GameState()

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
                outgoing.sendText(gameState.toMessage().toJson())
                delay(100)
            }
        }

        webSocket("/ws/command") {
            for (frame in incoming) {
                when (String(frame.data)) {
                    "KeyP" -> gameState.paused = !gameState.paused

                    "KeyW" -> gameState.ships.first().changeThrottle(BigDecimal(10))
                    "KeyS" -> gameState.ships.first().changeThrottle(BigDecimal(-10))
                    "KeyA" -> gameState.ships.first().changeRudder(BigDecimal(-10))
                    "KeyD" -> gameState.ships.first().changeRudder(BigDecimal(10))
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

class GameState {

    var time = GameTime()
    var paused = true
    val ships = mutableListOf(Ship())

    init {
        GlobalScope.launch {
            while (true) {
                update()
                delay(20)
            }
        }
    }

    fun toMessage() =
        GameStateMessage(
            paused = paused,
            ships = ships.map { it.toMessage() }
        )

    private fun update(delta: BigDecimal = BigDecimal("0.02")) {
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
    private var position = AtomicReference(Vector2())
    private var speed = AtomicReference(Vector2())
    private var rotation = AtomicReference(BigDecimal(90).toRadians())

    private var throttle = AtomicReference(BigDecimal.ZERO)
    private var thrust = AtomicReference(BigDecimal.ZERO)
    private var rudder = AtomicReference(BigDecimal.ZERO)

    private val history = mutableListOf<Pair<BigDecimal, Vector2>>()

    fun update(time: GameTime) {
        val diff = if (throttle.get() > thrust.get()) 10 else if (throttle.get() < thrust.get()) -10 else 0
        thrust.updateAndGet { it + diff.toBigDecimal() * time.delta }

        rotation.updateAndGet { (it + (rudder.get().toRadians() * PI * time.delta)).setScale(9, RoundingMode.FLOOR) }

        speed.updateAndGet {
            Vector2(thrust.get(), BigDecimal.ZERO).rotate(rotation.get()).setScale(9)
        }
        position.updateAndGet { (it + speed.get() * time.delta).setScale(9) }

        if (history.isEmpty()) {
            history.add(Pair(time.current, position.get()))
        } else {
            if ((history.last().first - time.current).abs() > BigDecimal(1)) {
                history.add(Pair(time.current, position.get()))
            }
            if (history.size > 10) {
                history.removeAt(0)
            }
        }
    }

    fun changeThrottle(diff: BigDecimal): BigDecimal = throttle.updateAndGet {
        (it + diff).constrain(BigDecimal(-100), BigDecimal(100))
    }

    fun changeRudder(diff: BigDecimal): BigDecimal = rudder.updateAndGet {
        (it + diff).constrain(BigDecimal(-100), BigDecimal(100))
    }

    fun toMessage() =
        ShipMessage(
            speed = speed.get(),
            position = position.get(),
            rotation = rotation.get(),
            throttle = throttle.get(),
            rudder = rudder.get(),
            history = mutableListOf<Pair<BigDecimal, Vector2>>().apply { addAll(history) }
        )
}
