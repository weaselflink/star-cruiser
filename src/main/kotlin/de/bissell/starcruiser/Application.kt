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
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@UnstableDefault
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val gameState = GameState(log)

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
                outgoing.sendText(gameState.createGameStateMessage().toJson())
                delay(100)
            }
        }

        webSocket("/ws/command") {
            for (frame in incoming) {
                val code = String(frame.data)
                log.warn("code: $code")
                when (code) {
                    "KeyW" -> gameState.changeThrottle(BigDecimal(10))
                    "KeyS" -> gameState.changeThrottle(BigDecimal(-10))
                }
            }
        }
    }
}

private suspend fun SendChannel<Frame>.sendText(value: String) = send(Frame.Text(value))

@Serializable
data class Vector(
    val x: BigDecimal = BigDecimal.ZERO,
    val y: BigDecimal = BigDecimal.ZERO
) {
    @UnstableDefault
    fun toJson(): String = Json.stringify(serializer(), this)
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
    fun toJson(): String = Json.stringify(GameStateMessage.serializer(), this)
}

@Serializable
data class ShipMessage(
    val position: Vector,
    val speed: Vector
)

class GameState(
    val log: Logger
) {

    private val dragFactor = BigDecimal("10")
    private val thrust = BigDecimal(1000)
    private val weight = BigDecimal(10)
    private var throttle = AtomicReference(BigDecimal.ZERO)
    private var x = AtomicReference(BigDecimal.ZERO)
    private var y = AtomicReference(BigDecimal.ZERO)
    private var vx = AtomicReference(BigDecimal.ZERO)
    private var vy = AtomicReference(BigDecimal.ZERO)

    val position: Vector
        get() = Vector(x.get(), y.get())
    val speed: Vector
        get() = Vector(vx.get(), vy.get())

    init {
        GlobalScope.launch {
            while (true) {
                update()
                delay(10)
            }
        }
    }

    fun createGameStateMessage() =
        GameStateMessage(
            ships = listOf(
                ShipMessage(
                    speed = speed,
                    position = position
                )
            )
        )

    fun changeThrottle(diff: BigDecimal): BigDecimal = throttle.updateAndGet {
        (it + diff).constrain(BigDecimal(-100), BigDecimal(100))
    }

    private fun update() {
        val delta = BigDecimal("0.01")
        val avy = vy.updateAndGet {
            val thrustForce = throttle.get().divide(BigDecimal(100), 6, RoundingMode.FLOOR) * thrust
            val dragForce = (it.signum().toBigDecimal()) * it * it * dragFactor
            val acceleration =
                thrustForce.divide(weight, 6, RoundingMode.FLOOR) - dragForce.divide(weight, 6, RoundingMode.FLOOR)
            log.warn("$throttle $thrustForce $dragForce $acceleration")
            (it + acceleration * delta).setScale(6, RoundingMode.FLOOR)
        }
        x.updateAndGet { (it + vx.get() * delta).setScale(6, RoundingMode.FLOOR) }
        y.updateAndGet { (it + avy * delta).setScale(6, RoundingMode.FLOOR) }
    }

    private fun BigDecimal.constrain(min: BigDecimal, max: BigDecimal) =
        if (this > max) max else if (this < min) min else this
}

