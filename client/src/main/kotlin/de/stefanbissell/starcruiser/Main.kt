package de.stefanbissell.starcruiser

import kotlinx.browser.window

val clientSocket = ClientSocket()

object ClientState {
    var showStationOverlay = false
    var rotateScope = false

    fun toggleRotateScope() {
        rotateScope = !rotateScope
    }

    fun toggleStationOverlay() {
        showStationOverlay = !showStationOverlay
    }
}

fun main() {
    window.onload = {
        MainUi()
        clientSocket.createSocket()
    }
}
