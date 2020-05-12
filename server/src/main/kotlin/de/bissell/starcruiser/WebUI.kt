package de.bissell.starcruiser

import azadev.kotlin.css.*
import azadev.kotlin.css.colors.hex
import azadev.kotlin.css.dimens.percent
import azadev.kotlin.css.dimens.px
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
import kotlinx.html.*

fun Routing.webUi() {
    static("/js") {
        resources("js")
    }

    get("/css/bla.css") {
        call.respondText(contentType = ContentType.Text.CSS) {
            Stylesheet {
                body {
                    margin = 0
                    padding = 0
                    backgroundColor = hex(0x202020)
                    color = hex(0xffffff)
                    fontFamily = "\"Courier New\", Courier, monospace"
                }
                canvas {
                    position = FIXED
                    top = 0
                    left = 0
                    width = 100.percent
                    height = 100.percent
                }
                c("topInfo") {
                    position = FIXED
                    top = 0
                    zIndex = 10
                    padding = 20.px
                }
                c("info") {
                    position = FIXED
                    bottom = 0
                    zIndex = 10
                    padding = 20.px
                }
                ul.c("playerShips").li {
                    listStyleType = NONE
                    margin = 3.px
                    paddingLeft = 5.px
                    paddingRight = 5.px
                    paddingTop = 3.px
                    paddingBottom = 3.px
                    backgroundColor = hex(0x000000)
                    borderRadius = 5.px
                    cursor = POINTER
                }
            }.render()
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
                    href = "/css/bla.css"
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
        div(classes = "topInfo") {
            p(classes = "conn") {
                +"disconnected"
            }
            p {
                +"Player Ships"
            }
            ul(classes = "playerShips") {}
        }
    }
}

private fun BODY.helmUi() {
    div {
        id = "helm"
        div(classes = "topInfo") {
            p(classes = "conn") {
                +"disconnected"
            }
            p {
                +"Player Ships"
            }
            ul(classes = "playerShips") {}
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
