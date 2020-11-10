package de.stefanbissell.starcruiser

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

object ClientState {
    var showStationOverlay = false
    var rotateScope = false
    val fullScreen
        get() = document.fullscreenElement != null
    fun toggleRotateScope() {
        rotateScope = !rotateScope
    }

    fun toggleStationOverlay() {
        showStationOverlay = !showStationOverlay
    }

    fun toggleFullscreen() {
        val body: HTMLElement = document.byQuery("body")
        if (fullScreen) {
            document.exitFullscreen()
        } else {
            body.requestFullscreen()
        }
    }
}

fun main() {
    window.onload = {
        MainUi()
        ClientSocket.createSocket()
    }
}
