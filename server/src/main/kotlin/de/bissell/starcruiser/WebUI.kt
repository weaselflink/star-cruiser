package de.bissell.starcruiser

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
import kotlinx.css.GridColumn
import kotlinx.css.GridRow
import kotlinx.css.GridTemplateColumns
import kotlinx.css.JustifyContent
import kotlinx.css.Outline
import kotlinx.css.Position.fixed
import kotlinx.css.TextAlign
import kotlinx.css.alignContent
import kotlinx.css.alignSelf
import kotlinx.css.backgroundColor
import kotlinx.css.body
import kotlinx.css.borderBottomLeftRadius
import kotlinx.css.borderBottomRightRadius
import kotlinx.css.borderColor
import kotlinx.css.borderRadius
import kotlinx.css.borderStyle
import kotlinx.css.borderTopLeftRadius
import kotlinx.css.borderTopRightRadius
import kotlinx.css.borderWidth
import kotlinx.css.bottom
import kotlinx.css.canvas
import kotlinx.css.color
import kotlinx.css.cursor
import kotlinx.css.display
import kotlinx.css.fontFamily
import kotlinx.css.fontSize
import kotlinx.css.fontWeight
import kotlinx.css.gap
import kotlinx.css.gridColumn
import kotlinx.css.gridRow
import kotlinx.css.gridTemplateColumns
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
import kotlinx.css.right
import kotlinx.css.textAlign
import kotlinx.css.top
import kotlinx.css.vh
import kotlinx.css.vmin
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
import kotlinx.html.meta
import kotlinx.html.p
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
        call.respondCss {
            body {
                margin = 0.vmin.value
                padding = 0.vmin.value
                backgroundColor = Color("#222")
                color = white
                fontFamily = "sans-serif"
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
            ".spawn" {
                marginBottom = 2.vmin
            }
            ".playerShips" {
                display = grid
                gap = Gap(1.vmin.value)
                alignSelf = Align.flexStart
            }
            "#destroyed-ui" {
                display = grid
                minHeight = 100.vh
                alignContent = Align.center
                justifyContent = JustifyContent.center
            }
            buttonCss()
            selectionDetailsCss()
            popupCss()
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
                destroyedUi()
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
            button(classes = "playerShipsPrev leftEdge") { +"Prev" }
            div(classes = "playerShips") {}
            button(classes = "playerShipsNext leftEdge") { +"Next" }
        }
    }
}

private fun BODY.destroyedUi() {
    div {
        id = "destroyed-ui"
        div(classes = "popup") {
            p {
                +"Your ship was destroyed"
            }
            button(classes = "toSelection") {
                +"To Selection"
            }
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
        div {
            id = "selection-details"
            div(classes = "designation") {}
            div { +"Bearing" }
            div(classes = "bearing") {}
            div { +"Range" }
            div(classes = "range") {}
            button(classes = "detailsScanButton") {
                +"Scan"
            }
            button(classes = "detailsDeleteButton") {
                +"Delete"
            }
        }
    }
}

private fun BODY.mainScreenUi() {
    div {
        id = "main-screen-ui"
        canvas {}
    }
}

private fun CSSBuilder.buttonCss() {
    ".topLeftButtons, .topRightButtons" {
        position = fixed
        display = grid
        gap = Gap(1.vmin.value)
        marginTop = 1.vmin
        top = 0.vmin
        zIndex = 10
        padding(4.vmin, 0.vmin)
    }
    ".topLeftButtons" {
        left = 0.vmin
    }
    ".topRightButtons" {
        right = 0.vmin
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
    "button.current" {
        color = black
        backgroundColor = darkGrey
    }
    "button:hover" {
        color = black
        backgroundColor = dimGrey
    }
    "button.current:hover" {
        color = black
        backgroundColor = Color("#c3c3c3")
    }
    "button.leftEdge" {
        paddingRight = 3.vmin
        paddingLeft = 2.vmin
        borderTopLeftRadius = 0.vmin
        borderTopRightRadius = 4.vmin
        borderBottomRightRadius = 4.vmin
        borderBottomLeftRadius = 0.vmin
    }
    "button.rightEdge" {
        paddingTop = 1.vmin
        paddingRight = 2.vmin
        paddingBottom = 1.5.vmin
        paddingLeft = 3.vmin
        borderTopLeftRadius = 4.vmin
        borderTopRightRadius = 0.vmin
        borderBottomRightRadius = 0.vmin
        borderBottomLeftRadius = 4.vmin
    }
    "button:active" {
        color = black
        backgroundColor = dimGrey
        borderColor = dimGrey
    }
}

private fun CSSBuilder.selectionDetailsCss() {
    "#selection-details" {
        position = fixed
        right = 0.vmin
        bottom = 0.vmin
        zIndex = 10
        display = grid
        gridTemplateColumns = GridTemplateColumns("auto 10vmin")
        gap = Gap("2vmin")
        padding(3.vmin, 6.vmin)
        color = lightGrey
        backgroundColor = black
        borderWidth = 0.5.vmin
        borderStyle = solid
        borderColor = darkGrey
        borderTopLeftRadius = 4.vmin
        fontWeight = bold
        fontSize = 3.vmin
    }
    "#selection-details button" {
        borderRadius = 4.vmin
        declarations["justifySelf"] = "center"
        paddingLeft = 4.vmin
        paddingRight = 4.vmin
    }
    "#selection-details .designation" {
        gridColumn = GridColumn("span 2")
    }
    "#selection-details .bearing" {
        textAlign = TextAlign.right
    }
    "#selection-details .range" {
        textAlign = TextAlign.right
    }
    "#selection-details .detailsScanButton" {
        gridRow = GridRow("4")
        gridColumn = GridColumn("1 / span 2")
    }
    "#selection-details .detailsDeleteButton" {
        gridRow = GridRow("4")
        gridColumn = GridColumn("1 / span 2")
    }
}

private fun CSSBuilder.popupCss() {
    ".popup" {
        zIndex = 10
        display = grid
        gap = Gap("2vmin")
        padding(3.vmin, 6.vmin)
        color = lightGrey
        backgroundColor = black
        borderWidth = 0.5.vmin
        borderStyle = solid
        borderColor = darkGrey
        borderRadius = 4.vmin
        fontWeight = bold
        fontSize = 4.vmin
        justifyContent = JustifyContent.center
    }
    ".popup button" {
        borderRadius = 4.vmin
        paddingLeft = 4.vmin
        paddingRight = 4.vmin
    }
}

private suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) =
    respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)

