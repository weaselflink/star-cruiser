package de.bissell.starcruiser

import de.bissell.starcruiser.GameClient.Companion.startGameClient
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.netty.EngineMain
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration

fun main(args: Array<String>): Unit = EngineMain.main(args)

@ObsoleteCoroutinesApi
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val gameStateActor = gameStateActor()

    launch {
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
        json(jsonConfiguration)
    }

    routing {
        webUi()

        webSocket("/ws/client") {
            startGameClient(
                gameStateActor = gameStateActor,
                outgoing = outgoing,
                incoming = incoming
            )
        }
    }
}
