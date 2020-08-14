package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ApplicationConfig.gameStateUpdateIntervalMillis
import de.stefanbissell.starcruiser.GameState.Companion.gameStateActor
import de.stefanbissell.starcruiser.client.GameClient.Companion.startGameClient
import de.stefanbissell.starcruiser.client.createStatisticsActor
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration

fun main(args: Array<String>): Unit = EngineMain.main(args)

object ApplicationConfig {

    const val gameStateUpdateIntervalMillis: Long = 20
    const val gameClientUpdateIntervalMillis: Long = 10
    const val gameClientMaxInflightMessages: Int = 3
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    val gameStateActor = gameStateActor()
    val statisticsActor = createStatisticsActor()

    launch {
        while (isActive) {
            gameStateActor.send(Update)
            delay(gameStateUpdateIntervalMillis)
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        configuredJson
    }

    routing {
        webUi()

        webSocket("/ws/client") {
            startGameClient(
                gameStateActor = gameStateActor,
                statisticsActor = statisticsActor,
                outgoing = outgoing,
                incoming = incoming
            )
        }
    }
}
