package de.bissell.starcruiser

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.LinkHeader
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.css.*
import kotlinx.css.BorderStyle.solid
import kotlinx.css.Color.Companion.black
import kotlinx.css.Color.Companion.darkGrey
import kotlinx.css.Color.Companion.dimGrey
import kotlinx.css.Color.Companion.lightGrey
import kotlinx.css.Color.Companion.white
import kotlinx.css.Cursor.pointer
import kotlinx.css.Display.block
import kotlinx.css.Display.grid
import kotlinx.css.FontWeight.Companion.bold
import kotlinx.css.Position.fixed
import kotlinx.html.BODY
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

fun Routing.webUi() {
    static("/js") {
        resources("js")
    }

    static("/assets") {
        resources("assets")
    }

    get("/status") {
        call.respondText("alive")
    }

    get("/css/client.css") {
        call.respondText(contentType = ContentType.Text.CSS) {
            CSSBuilder(indent = "\t").apply {
                body {
                    margin = 0.px.value
                    padding = 0.px.value
                    backgroundColor = Color("#222")
                    color = white
                    fontFamily = "\"Courier New\", Courier, monospace"
                }
                "::backdrop" {
                    backgroundColor = Color("#222")
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
                ".topLeftButtons, .topRightButtons" {
                    position = fixed
                    display = grid
                    gap = Gap(10.px.value)
                    marginTop = 20.px
                    top = 0.px
                    zIndex = 10
                    padding(20.px, 0.px)
                }
                ".topLeftButtons" {
                    left = 0.px
                }
                ".topRightButtons" {
                    right = 0.px
                }
                ".playerShips" {
                    display = grid
                    gap = Gap(10.px.value)
                    marginTop = 20.px
                }
                "button" {
                    display = block
                    color = lightGrey
                    backgroundColor = black
                    textAlign = TextAlign.left
                    borderWidth = 3.px
                    borderStyle = solid
                    borderColor = darkGrey
                    cursor = pointer
                    fontWeight = bold
                    fontSize = 3.vmin
                    paddingTop = 1.vmin
                    paddingBottom = 1.5.vmin
                }
                "button.current" {
                    color = black
                    backgroundColor = darkGrey
                }
                "button.current::hover" {
                    color = black
                    backgroundColor = darkGrey
                }
                "button.leftEdge" {
                    paddingRight = 3.vmin
                    paddingLeft = 2.vmin
                    borderTopLeftRadius = 0.px
                    borderTopRightRadius = 4.vmin
                    borderBottomRightRadius = 4.vmin
                    borderBottomLeftRadius = 0.px
                }
                "button.rightEdge" {
                    paddingTop = 1.vmin
                    paddingRight = 2.vmin
                    paddingBottom = 1.5.vmin
                    paddingLeft = 3.vmin
                    borderTopLeftRadius = 4.vmin
                    borderTopRightRadius = 0.px
                    borderBottomRightRadius = 0.px
                    borderBottomLeftRadius = 4.vmin
                }
                "button:hover" {
                    color = black
                    backgroundColor = darkGrey
                }
                "button:active" {
                    color = black
                    backgroundColor = dimGrey
                    borderColor = dimGrey
                }
            }.toString()
        }
    }

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
                joinUi()
                commonShipUi()
                helmUi()
                weaponsUI()
                navigationUi()
                mainScreenUi()
            }
        }
    }
}

private fun BODY.joinUi() {
    div {
        id = "join-ui"
        div(classes = "topLeftButtons") {
            button(classes = "spawn leftEdge") {
                +"+ Spawn ship"
            }
            div(classes = "playerShips") {}
        }
    }
}

private fun BODY.commonShipUi() {
    div {
        id = "common-ship-ui"
        div(classes = "topLeftButtons") {
            button(classes = "exit leftEdge") {
                +"Exit ship"
            }
            button(classes = "fullscreen leftEdge") {
                +"Fullscreen"
            }
            button(classes = "extraButton rotateScope leftEdge") {
                +"Rotate scope"
            }
            button(classes = "extraButton lockTarget leftEdge") {
                +"Lock target"
            }
            button(classes = "extraButton addWaypoint leftEdge") {
                +"Add waypoint"
            }
            button(classes = "extraButton deleteWaypoint leftEdge") {
                +"Delete waypoint"
            }
            button(classes = "extraButton scanShip leftEdge") {
                +"Start scan"
            }
            button(classes = "extraButton topView leftEdge") {
                +"Top view"
            }
        }
        div(classes = "topRightButtons") {
            button(classes = "current switchToHelm rightEdge") {
                +"Helm"
            }
            button(classes = "switchToWeapons rightEdge") {
                +"Weapons"
            }
            button(classes = "switchToNavigation rightEdge") {
                +"Navigation"
            }
            button(classes = "switchToMainScreen rightEdge") {
                +"Main screen"
            }
        }
    }
}

private fun BODY.helmUi() {
    div {
        id = "helm-ui"
        canvas {}
    }
}

private fun BODY.weaponsUI() {
    div {
        id = "weapons-ui"
        canvas {}
    }
}

private fun BODY.navigationUi() {
    div {
        id = "navigation-ui"
        canvas {}
    }
}

private fun BODY.mainScreenUi() {
    div {
        id = "main-screen-ui"
        canvas {}
    }
}
