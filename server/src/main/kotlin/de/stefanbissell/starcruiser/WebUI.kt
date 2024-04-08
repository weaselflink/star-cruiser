package de.stefanbissell.starcruiser

import io.ktor.http.LinkHeader
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.html.BODY
import kotlinx.html.DIV
import kotlinx.html.ScriptType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.canvas
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.style

fun Routing.webUi() {
    staticResources("/css", "css")
    staticResources("/js", "js")
    staticResources("/assets", "assets")

    get("/") {
        call.respondHtml {
            head {
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1"
                }
                script {
                    type = ScriptType.textJavaScript
                    src = "/js/three.min.js"
                }
                script {
                    type = ScriptType.textJavaScript
                    src = "/js/gltf-loader.min.js"
                }
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
                canvas(classes = "canvas2d") {}
                canvas(classes = "canvas3d") {}
                joinUi
            }
        }
    }
}

private val BODY.joinUi
    get() = htmlUi("join-ui") {
        div(classes = "topLeftButtons") {
            button(classes = "spawn leftEdge") {
                style = "margin-bottom = 2vmin;"
                +"+ Spawn ship"
            }
            button(classes = "playerShipsPrev leftEdge") { +"Prev" }
            div(classes = "playerShips") {}
            button(classes = "playerShipsNext leftEdge") { +"Next" }
        }
    }

private fun BODY.htmlUi(divId: String, block: DIV.() -> Unit = {}) {
    div {
        id = divId
        block()
    }
}
