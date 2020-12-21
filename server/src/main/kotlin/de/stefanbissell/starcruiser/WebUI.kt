package de.stefanbissell.starcruiser

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.LinkHeader
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.css.Align
import kotlinx.css.BorderStyle.solid
import kotlinx.css.CSSBuilder
import kotlinx.css.Color
import kotlinx.css.Color.Companion.black
import kotlinx.css.Color.Companion.darkGrey
import kotlinx.css.Color.Companion.dimGrey
import kotlinx.css.Color.Companion.lightGrey
import kotlinx.css.Color.Companion.white
import kotlinx.css.Cursor.pointer
import kotlinx.css.Display.block
import kotlinx.css.Display.grid
import kotlinx.css.FontWeight.Companion.bold
import kotlinx.css.Gap
import kotlinx.css.JustifyContent
import kotlinx.css.Outline
import kotlinx.css.Position.absolute
import kotlinx.css.Position.fixed
import kotlinx.css.TextAlign
import kotlinx.css.alignContent
import kotlinx.css.alignSelf
import kotlinx.css.backgroundColor
import kotlinx.css.body
import kotlinx.css.borderBottomLeftRadius
import kotlinx.css.borderBottomRightRadius
import kotlinx.css.borderColor
import kotlinx.css.borderStyle
import kotlinx.css.borderTopLeftRadius
import kotlinx.css.borderTopRightRadius
import kotlinx.css.borderWidth
import kotlinx.css.canvas
import kotlinx.css.color
import kotlinx.css.cursor
import kotlinx.css.display
import kotlinx.css.fontFamily
import kotlinx.css.fontSize
import kotlinx.css.fontWeight
import kotlinx.css.gap
import kotlinx.css.height
import kotlinx.css.justifyContent
import kotlinx.css.left
import kotlinx.css.margin
import kotlinx.css.marginBottom
import kotlinx.css.marginTop
import kotlinx.css.minHeight
import kotlinx.css.outline
import kotlinx.css.padding
import kotlinx.css.paddingBottom
import kotlinx.css.paddingLeft
import kotlinx.css.paddingRight
import kotlinx.css.paddingTop
import kotlinx.css.pct
import kotlinx.css.position
import kotlinx.css.textAlign
import kotlinx.css.top
import kotlinx.css.vh
import kotlinx.css.vmin
import kotlinx.css.width
import kotlinx.css.zIndex
import kotlinx.html.BODY
import kotlinx.html.DIV
import kotlinx.html.FlowOrMetaDataContent
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
    static("/js") {
        resources("js")
    }

    static("/assets") {
        resources("assets")
    }

    get("/css/client.css") {
        call.respondCss {
            body {
                margin = 0.vmin.value
                padding = 0.vmin.value
                backgroundColor = Color("#222")
                color = white
                fontFamily = "sans-serif"
            }
            "body .canvas2d" {
                zIndex = 2
            }
            "body .canvas3d" {
                zIndex = 1
            }
            "::backdrop" {
                backgroundColor = Color("#222")
            }
            canvas {
                position = fixed
                top = 0.vmin
                left = 0.vmin
                width = 100.pct
                height = 100.pct
            }
            ".playerShips" {
                display = grid
                gap = Gap(1.vmin.value)
                alignSelf = Align.flexStart
            }
            buttonCss
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
                styleCss {
                    marginBottom = 2.vmin
                }
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

private val CSSBuilder.buttonCss: Unit
    get() {
        "#join-ui" {
            position = absolute
            zIndex = 10
        }
        "#destroyed-ui" {
            position = absolute
            zIndex = 10
            width = 100.pct
            display = grid
            minHeight = 100.vh
            alignContent = Align.center
            justifyContent = JustifyContent.center
        }
        ".topLeftButtons" {
            position = fixed
            display = grid
            gap = Gap(1.vmin.value)
            marginTop = 1.vmin
            top = 0.vmin
            left = 0.vmin
            padding(4.vmin, 0.vmin)
        }
        "button" {
            display = block
            color = lightGrey
            backgroundColor = black
            textAlign = TextAlign.left
            borderWidth = 0.5.vmin
            borderStyle = solid
            borderColor = darkGrey
            cursor = pointer
            fontWeight = bold
            fontSize = 3.vmin
            paddingTop = 1.vmin
            paddingBottom = 1.5.vmin
            outline = Outline.none
        }
        "button:hover" {
            color = black
            backgroundColor = dimGrey
        }
        "button.leftEdge" {
            paddingRight = 3.vmin
            paddingLeft = 2.vmin
            borderTopLeftRadius = 0.vmin
            borderTopRightRadius = 4.vmin
            borderBottomRightRadius = 4.vmin
            borderBottomLeftRadius = 0.vmin
        }
        "button:active" {
            color = black
            backgroundColor = dimGrey
            borderColor = dimGrey
        }
    }

@Suppress("DEPRECATION")
private fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

private suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) =
    respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
