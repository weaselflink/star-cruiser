package de.bissell.starcruiser

import azadev.kotlin.css.Stylesheet
import azadev.kotlin.css.backgroundColor
import azadev.kotlin.css.color
import azadev.kotlin.css.colors.hex
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.LinkHeader
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.*

fun Routing.webUi() {
    static("static") {
        files("js")
    }

    get("/css/bla.css") {
        call.respondText(contentType = ContentType.Text.CSS) {
            Stylesheet {
                body {
                    backgroundColor = hex(0x000000)
                    color = hex(0xffffff)
                }
            }.render()
        }
    }

    get("/") {
        call.respondHtml {
            head {
                script {
                    type = ScriptType.textJavaScript
                    src = "/static/bla.js"
                }
                link {
                    rel = LinkHeader.Rel.Stylesheet
                    href = "/css/bla.css"
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
                    width = "400px"
                    height = "400px"
                }
                p {
                    +"Heading: "
                    span {
                        id = "heading"
                        +"unknown"
                    }
                    +"\tVelocity: "
                    span {
                        id = "velocity"
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
}
