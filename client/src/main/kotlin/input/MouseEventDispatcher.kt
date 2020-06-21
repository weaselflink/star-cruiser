package input

import de.bissell.starcruiser.Vector2
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

interface MouseEventHandler {
    fun isInterestedIn(pointerEvent: PointerEvent): Boolean = true

    fun handlePointerDown(pointerEvent: PointerEvent) {}

    fun handlePointerMove(pointerEvent: PointerEvent) {}

    fun handlePointerUp(pointerEvent: PointerEvent) {}
}

class MouseEventDispatcher(
    canvas: HTMLCanvasElement
) {

    private val handlers = mutableListOf<MouseEventHandler>()
    private var currentMouseHandler: MouseEventHandler? = null

    init {
        canvas.onmousedown = { handleMouseDown(it) }
        canvas.onmousemove = { handleMouseMove(it) }
        canvas.onmouseup = { handleMouseUp(it) }
        canvas.onmouseleave = { handleMouseUp(it) }
    }

    fun addHandler(handler: MouseEventHandler) {
        handlers += handler
    }

    private fun handleMouseDown(mouseEvent: MouseEvent) {
        currentMouseHandler = handlers
            .firstOrNull { it.isInterestedIn(PointerEvent(mouseEvent.toVector2())) }
            ?.apply {
                handlePointerDown(
                    PointerEvent(mouseEvent.toVector2())
                )
            }
    }

    private fun handleMouseMove(mouseEvent: MouseEvent) {
        currentMouseHandler?.handlePointerMove(mouseEvent.toPointerEvent())
    }

    private fun handleMouseUp(mouseEvent: MouseEvent) {
        currentMouseHandler?.handlePointerUp(mouseEvent.toPointerEvent())
        currentMouseHandler = null
    }
}

data class PointerEvent(
    val point: Vector2
)

private fun MouseEvent.toPointerEvent() = PointerEvent(toVector2())

fun MouseEvent.toVector2() = Vector2(offsetX, offsetY)
