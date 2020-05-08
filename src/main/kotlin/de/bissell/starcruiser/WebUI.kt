package de.bissell.starcruiser

import azadev.kotlin.css.*
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
import kotlinx.html.ScriptType
import kotlinx.html.body
import kotlinx.html.canvas
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.ul

fun Routing.webUi() {
    static("static") {
        files("js")
    }

    get("/css/bla.css") {
        call.respondText(contentType = ContentType.Text.CSS) {
            Stylesheet {
                body {
                    margin = 0
                    padding = 0
                    backgroundColor = hex(0x333333)
                    color = hex(0xffffff)
                    fontFamily = "Arial, Helvetica, sans-serif"
                }
                canvas {
                    position = "fixed"
                    top = 0
                    left = 0
                    width = "100%"
                    height = "100%"
                }
                id("topInfo") {
                    position = "fixed"
                    top = 0
                    zIndex = 10
                    padding = "20px"
                }
                id("info") {
                    position = "fixed"
                    bottom = 0
                    zIndex = 10
                    padding = "20px"
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
                div {
                    id = "topInfo"
                    p {
                        id = "conn"
                        +"disconnected"
                    }
                    p {
                        +"Player Ships"
                    }
                    ul {
                        id = "playerShips"
                    }
                }
                canvas {
                    id = "canvas"
                }
                div {
                    id = "info"
                    p {
                        +"Heading: "
                        span {
                            id = "heading"
                            +"unknown"
                        }
                    }
                    p {
                        +"Velocity: "
                        span {
                            id = "velocity"
                            +"unknown"
                        }
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
