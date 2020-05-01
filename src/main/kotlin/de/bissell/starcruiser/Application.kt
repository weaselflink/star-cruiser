@file:UseSerializers(BigDecimalSerializer::class)

package de.bissell.starcruiser

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
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
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI

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
                        +"Position: "
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
            while (true) {
                outgoing.sendText(gameState.toMessage().toJson())
                delay(100)
            }
        }

        webSocket("/ws/command") {
            for (frame in incoming) {
                val code = String(frame.data)
                log.warn("code: $code")
                when (code) {
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
data class Vector2(
    val x: BigDecimal = BigDecimal.ZERO,
    val y: BigDecimal = BigDecimal.ZERO
) {

    operator fun plus(other: Vector2): Vector2 =
        Vector2(x + other.x, y + other.y)

    operator fun times(other: BigDecimal): Vector2 =
        Vector2(x * other, y * other)

    fun setScale(scale: Int): Vector2 =
        Vector2(x.setScale(scale, RoundingMode.FLOOR), y.setScale(scale, RoundingMode.FLOOR))
}

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

@Serializable
data class GameStateMessage(
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
    val rudder: BigDecimal
)

class GameState {

    val ships = mutableListOf(Ship())

    init {
        GlobalScope.launch {
            while (true) {
                update()
                delay(10)
            }
        }
    }

    fun toMessage() =
        GameStateMessage(
            ships = ships.map { it.toMessage() }
        )

    private fun update(delta: BigDecimal = BigDecimal("0.01")) {
        ships.forEach { it.update(delta) }
    }
}

class Ship {
    private var position = AtomicReference(Vector2())
    private var speed = AtomicReference(Vector2())
    private var rotation = AtomicReference(BigDecimal.ZERO)

    private var throttle = AtomicReference(BigDecimal.ZERO)
    private var rudder = AtomicReference(BigDecimal.ZERO)

    fun update(delta: BigDecimal) {
        rotation.updateAndGet { it + (rudder.get() * BigDecimal.valueOf(PI) / BigDecimal(180) * delta) }
        speed.updateAndGet {
            val diff = if (throttle.get() > it.y) 10 else if (throttle.get() < it.y) -10 else 0
            Vector2(BigDecimal.ZERO, it.y + diff.toBigDecimal() * delta)
        }
        position.updateAndGet { (it + speed.get() * delta).setScale(6) }
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
            rudder = rudder.get()
        )
}

private fun BigDecimal.constrain(min: BigDecimal, max: BigDecimal) =
    min(max, max(min, this))

private fun BigDecimal.isZero() =
    this.compareTo(BigDecimal.ZERO) == 0

private fun min(a: BigDecimal, b: BigDecimal) =
    if (a > b) b else a

private fun max(a: BigDecimal, b: BigDecimal) =
    if (a > b) a else b

