package de.bissell.starcruiser

import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.LinkHeader
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.css.BorderStyle.solid
import kotlinx.css.CSSBuilder
import kotlinx.css.Color
import kotlinx.css.Color.Companion.black
import kotlinx.css.Color.Companion.grey
import kotlinx.css.Color.Companion.white
import kotlinx.css.Cursor.pointer
import kotlinx.css.Display.block
import kotlinx.css.Display.grid
import kotlinx.css.FontWeight.Companion.bold
import kotlinx.css.Gap
import kotlinx.css.Position.fixed
import kotlinx.css.TextAlign.center
import kotlinx.css.backgroundColor
import kotlinx.css.body
import kotlinx.css.border
import kotlinx.css.borderRadius
import kotlinx.css.bottom
import kotlinx.css.canvas
import kotlinx.css.color
import kotlinx.css.cursor
import kotlinx.css.display
import kotlinx.css.fontFamily
import kotlinx.css.fontSize
import kotlinx.css.fontWeight
import kotlinx.css.gap
import kotlinx.css.height
import kotlinx.css.left
import kotlinx.css.margin
import kotlinx.css.padding
import kotlinx.css.pct
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.right
import kotlinx.css.textAlign
import kotlinx.css.top
import kotlinx.css.width
import kotlinx.css.zIndex
import kotlinx.html.BODY
import kotlinx.html.ScriptType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.canvas
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.span

fun Routing.webUi() {
    static("/js") {
        resources("js")
    }

    get("/css/client.css") {
        call.respondText(contentType = ContentType.Text.CSS) {
            CSSBuilder(indent = "\t").apply {
                body {
                    margin = 0.px.value
                    padding = 0.px.value
                    backgroundColor = Color("#202020")
                    color = white
                    fontFamily = "\"Courier New\", Courier, monospace"
                }
                canvas {
                    position = fixed
                    top = 0.px
                    left = 0.px
                    width = 100.pct
                    height = 100.pct
                }
                ".conn" {
                    position = fixed
                    top = 0.px
                    right = 0.px
                    zIndex = 10
                    padding = 20.px.value
                }
                ".topInfo" {
                    position = fixed
                    top = 0.px
                    zIndex = 10
                    padding = 20.px.value
                }
                ".info" {
                    position = fixed
                    bottom = 0.px
                    zIndex = 10
                    padding = 20.px.value
                }
                ".playerShips" {
                    display = grid
                    gap = Gap(10.px.value)
                }
                "button" {
                    display = block
                    color = white
                    backgroundColor = black
                    textAlign = center
                    padding = "10px"
                    border = "${1.px} $solid $white"
                    borderRadius = 5.px
                    cursor = pointer
                    fontWeight = bold
                    fontSize = 16.px
                }
                "button:hover" {
                    color = black
                    backgroundColor = white
                }
                "button:active" {
                    color = black
                    backgroundColor = grey
                }
            }.toString()
        }
    }

    get("/") {
        call.respondHtml {
            head {
                script {
                    type = ScriptType.textJavaScript
                    src = "/js/client.js"
                }
                link {
                    rel = LinkHeader.Rel.Stylesheet
                    href = "/css/client.css"
                }
            }
            body {
                joinUi()
                helmUi()
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

private fun BODY.joinUi() {
    div {
        id = "join"
        div(classes = "conn") {
            +"disconnected"
        }
        div(classes = "topInfo") {
            p {
                +"Player Ships"
            }
            div(classes = "playerShips") {}
        }
    }
}

private fun BODY.helmUi() {
    div {
        id = "helm"
        div(classes = "conn") {
            +"disconnected"
        }
        div(classes = "topInfo") {
            button(classes = "exit") {
                +"< Exit ship"
            }
        }
        canvas {
            id = "canvas"
        }
        div(classes = "info") {
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
