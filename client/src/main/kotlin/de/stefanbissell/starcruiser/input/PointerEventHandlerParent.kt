package de.stefanbissell.starcruiser.input

open class PointerEventHandlerParent : PointerEventHandler {

    private val handlers = mutableListOf<PointerEventHandler>()
    private var currentHandler: PointerEventHandler? = null

    fun addChildren(vararg handlersToAdd: PointerEventHandler) {
        handlersToAdd.forEach { handlers += it }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return handlers.any { it.isInterestedIn(pointerEvent) }
    }

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        handlers
            .firstOrNull { it.isInterestedIn(pointerEvent) }
            ?.apply {
                handlePointerDown(pointerEvent)
            }
            ?.also {
                currentHandler = it
            }
    }

    override fun handlePointerMove(pointerEvent: PointerEvent) {
        currentHandler?.handlePointerMove(pointerEvent)
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        currentHandler?.handlePointerUp(pointerEvent)
        currentHandler = null
    }
}
