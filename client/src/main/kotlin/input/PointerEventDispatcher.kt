package input

import de.bissell.starcruiser.Vector2
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Touch
import org.w3c.dom.TouchEvent
import org.w3c.dom.asList
import org.w3c.dom.events.MouseEvent

interface PointerEventHandler {
    fun isInterestedIn(pointerEvent: PointerEvent): Boolean = true

    fun handlePointerDown(pointerEvent: PointerEvent) {}

    fun handlePointerMove(pointerEvent: PointerEvent) {}

    fun handlePointerUp(pointerEvent: PointerEvent) {}
}

class PointerEventDispatcher(
    canvas: HTMLCanvasElement
) {

    private val handlers = mutableListOf<PointerEventHandler>()
    private var currentMouseHandler: PointerEventHandler? = null
    private var currentTouchHandlers: MutableMap<Int, PointerEventHandler> = mutableMapOf()

    init {
        canvas.onmousedown = { handleMouseDown(it) }
        canvas.onmousemove = { handleMouseMove(it) }
        canvas.onmouseup = { handleMouseUp(it) }
        canvas.onmouseleave = { handleMouseUp(it) }
        canvas.addEventListener(
            type = "touchstart",
            callback = { handleTouchDown(it as TouchEvent) }
        )
        canvas.addEventListener(
            type = "touchmove",
            callback = { handleTouchMove(it as TouchEvent) }
        )
        canvas.addEventListener(
            type = "touchend",
            callback = { handleTouchUp(it as TouchEvent) }
        )
        canvas.addEventListener(
            type = "touchcancel",
            callback = { handleTouchUp(it as TouchEvent) }
        )
    }

    fun addHandlers(vararg handlersToAdd: PointerEventHandler) {
        handlersToAdd.forEach { handlers += it }
    }

    private fun handleMouseDown(mouseEvent: MouseEvent) {
        val event = mouseEvent.toPointerEvent()
        handlers
            .firstOrNull { it.isInterestedIn(event) }
            ?.apply {
                handlePointerDown(event)
            }
            ?.also {
                currentMouseHandler = it
            }
    }

    private fun handleMouseMove(mouseEvent: MouseEvent) {
        currentMouseHandler?.handlePointerMove(mouseEvent.toPointerEvent())
    }

    private fun handleMouseUp(mouseEvent: MouseEvent) {
        currentMouseHandler?.handlePointerUp(mouseEvent.toPointerEvent())
        currentMouseHandler = null
    }

    private fun handleTouchDown(touchEvent: TouchEvent) {
        touchEvent.changedTouches.asList().forEach { touch ->
            val event = touch.toPointerEvent()
            handlers
                .firstOrNull { it.isInterestedIn(event) }
                ?.apply {
                    handlePointerDown(event)
                }
                ?.also {
                    currentTouchHandlers[touch.identifier] = it
                }
        }
    }

    private fun handleTouchMove(touchEvent: TouchEvent) {
        touchEvent.changedTouches.asList().forEach { touch ->
            currentTouchHandlers[touch.identifier]?.handlePointerMove(touch.toPointerEvent())
        }
    }

    private fun handleTouchUp(touchEvent: TouchEvent) {
        touchEvent.changedTouches.asList().forEach { touch ->
            currentTouchHandlers[touch.identifier]?.handlePointerUp(touch.toPointerEvent())
            currentTouchHandlers.remove(touch.identifier)
        }
    }
}

data class PointerEvent(
    val point: Vector2
)

private fun MouseEvent.toPointerEvent() = PointerEvent(toVector2())

private fun Touch.toPointerEvent() = PointerEvent(toVector2())

fun MouseEvent.toVector2() = Vector2(offsetX, offsetY)

fun Touch.toVector2() = Vector2(clientX, clientY)
