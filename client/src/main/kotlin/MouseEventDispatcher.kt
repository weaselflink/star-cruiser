import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

interface MouseEventHandler {
    fun isInterestedIn(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Boolean = true

    fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {}

    fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {}

    fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {}
}

class MouseEventDispatcher(
    private val canvas: HTMLCanvasElement
) : MouseEventHandler {

    private val handlers = mutableListOf<MouseEventHandler>()
    private var currentHandler: MouseEventHandler? = null

    init {
        canvas.onmousedown = { handleMouseDown(canvas, it) }
        canvas.onmousemove = { handleMouseMove(canvas, it) }
        canvas.onmouseup = { handleMouseUp(canvas, it) }
    }

    fun addHandler(handler: MouseEventHandler) {
        handlers += handler
    }

    override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        currentHandler = handlers.firstOrNull { it.isInterestedIn(canvas, mouseEvent) }?.apply {
            handleMouseDown(canvas, mouseEvent)
        }
    }

    override fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        currentHandler?.handleMouseMove(canvas, mouseEvent)
    }

    override fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        currentHandler?.handleMouseUp(canvas, mouseEvent)
        currentHandler = null
    }
}
