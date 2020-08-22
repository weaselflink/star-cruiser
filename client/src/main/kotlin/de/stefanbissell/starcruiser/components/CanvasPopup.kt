package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawRect
import org.w3c.dom.BOTTOM
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement

class CanvasPopup(
    private val canvas: HTMLCanvasElement
) {

    private val ctx = canvas.context2D

    fun draw(
        dim: ComponentDimensions,
        title: String
    ) {
        with(ctx) {
            save()

            fillStyle = UiStyle.buttonBackgroundColor
            lineWidth = UiStyle.buttonLineWidth.vmin
            beginPath()
            drawRect(dim)
            fill()

            strokeStyle = UiStyle.buttonForegroundColor
            beginPath()
            drawRect(dim)
            stroke()

            drawTitle(dim, title)

            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawTitle(
        dim: ComponentDimensions,
        title: String
    ) {
        val x = dim.bottomX + 5.vmin
        val y = dim.bottomY - dim.height + 8.vmin

        save()

        font = UiStyle.font(5.vmin)
        textBaseline = CanvasTextBaseline.BOTTOM
        fillStyle = UiStyle.buttonForegroundColor
        fillText(title, x, y)

        restore()
    }

    private val Int.vmin
        get() = canvas.dimensions().vmin * this

    private val Double.vmin
        get() = canvas.dimensions().vmin * this
}
