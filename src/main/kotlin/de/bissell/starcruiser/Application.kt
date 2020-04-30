package de.bissell.starcruiser

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.auth.*
import io.ktor.html.respondHtml
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.files
import io.ktor.http.content.static
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.*
import java.time.*
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
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
                outgoing.send(Frame.Text("""{ "x": ${GameState.x.get()}, "y": ${GameState.y.get()} }"""))
                delay(100)
            }
        }

        webSocket("/ws/command") {
            for (frame in incoming) {
                val keyCode = String(frame.data)
                when (keyCode) {
                    "37" -> GameState.addXSpeed(-5)
                    "39" -> GameState.addXSpeed(5)
                    "38" -> GameState.addYSpeed(5)
                    "40" -> GameState.addYSpeed(-5)
                }
            }
        }
    }
}

object GameState {
    var x = AtomicLong()
    var y = AtomicLong()
    var vx = AtomicLong()
    var vy = AtomicLong()

    init {
        GlobalScope.launch {
            while (true) {
                update(10)
                delay(10)
            }
        }
    }

    fun addXSpeed(value: Long) = vx.updateAndGet { it + value }
    fun addYSpeed(value: Long) = vy.updateAndGet { it + value }

    private fun update(delta: Long) {
        x.updateAndGet { it + vx.get() * delta }
        y.updateAndGet { it + vy.get() * delta }
    }
}

