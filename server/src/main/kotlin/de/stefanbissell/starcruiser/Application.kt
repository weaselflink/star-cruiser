package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ApplicationConfig.gameStateUpdateIntervalMillis
import de.stefanbissell.starcruiser.client.GameClient.Companion.startGameClient
import de.stefanbissell.starcruiser.client.StatisticsMessage
import de.stefanbissell.starcruiser.client.StatisticsSnapshot
import de.stefanbissell.starcruiser.client.statisticsActor
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun main(args: Array<String>): Unit = EngineMain.main(args)

object ApplicationConfig {

    const val gameStateUpdateIntervalMillis: Long = 20
    const val gameClientUpdateIntervalMillis: Long = 10
    const val gameClientMaxInflightMessages: Int = 3
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    log

    val gameStateActor = gameStateActor()
    val statisticsActor = statisticsActor()

    launch {
        while (isActive) {
            gameStateActor.send(Update)
            delay(gameStateUpdateIntervalMillis)
        }
    }

    install(WebSockets) {
        pingPeriod = 15.seconds.toJavaDuration()
        timeout = 15.seconds.toJavaDuration()
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
                log = this@module.log
            )
        }
    }
}
