package de.stefanbissell.starcruiser

import kotlinx.browser.window

val clientSocket = ClientSocket()

object ClientState {
    var rotateScope = false

    fun toggleRotateScope() {
        rotateScope = !rotateScope
    }
}

fun main() {
    window.onload = {
        MainUi()
        clientSocket.createSocket()
    }
}
