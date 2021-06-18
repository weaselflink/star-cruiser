package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ApplicationConfig.gameStateUpdateIntervalMillis
import de.stefanbissell.starcruiser.client.GameClient.Companion.startGameClient
import de.stefanbissell.starcruiser.client.StatisticsMessage
import de.stefanbissell.starcruiser.client.StatisticsSnapshot
import de.stefanbissell.starcruiser.client.statisticsActor
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.netty.EngineMain
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun main(args: Array<String>): Unit = EngineMain.main(args)

object ApplicationConfig {

    const val gameStateUpdateIntervalMillis: Long = 20
    const val gameClientUpdateIntervalMillis: Long = 10
    const val gameClientMaxInflightMessages: Int = 3
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    val gameStateActor = gameStateActor()
    val statisticsActor = statisticsActor()

    launch {
        while (isActive) {
            gameStateActor.send(Update)
            delay(gameStateUpdateIntervalMillis)
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.seconds(15).toJavaDuration()
        timeout = Duration.seconds(15).toJavaDuration()
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        json(configuredJson)
    }

    routing {
        webUi()

        get("/restart") {
            gameStateActor.send(Restart)
            call.respondRedirect("/")
        }

        statusRoute()

        get("/statistics") {
            val response = CompletableDeferred<StatisticsSnapshot>()
            statisticsActor.send(StatisticsMessage.GetSnapshot(response))
            call.respondText(response.await().toJson())
        }

        webSocket("/ws/client") {
            startGameClient(
                gameStateActor = gameStateActor,
                statisticsActor = statisticsActor,
                outgoing = outgoing,
                incoming = incoming,
                log = log
            )
        }
    }
}
