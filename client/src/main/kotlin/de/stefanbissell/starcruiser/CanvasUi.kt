package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.StationUi
import de.stefanbissell.starcruiser.input.PointerEventDispatcher
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document

open class CanvasUi(
    override val station: Station,
    wrapperId: String
) : StationUi {

    private val root = document.getHtmlElementById(wrapperId)
    val canvas: HTMLCanvasElement = root.canvas
    val ctx = canvas.context2D
    val pointerEventDispatcher = PointerEventDispatcher(canvas)

    init {
        resize()
    }

    fun resize() {
        canvas.updateSize()
    }

    override fun show() {
        root.visibility = Visibility.visible
    }

    override fun hide() {
        root.visibility = Visibility.hidden
    }
}