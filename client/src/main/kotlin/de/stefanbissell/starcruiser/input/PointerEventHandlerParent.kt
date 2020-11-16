package de.stefanbissell.starcruiser.input

open class PointerEventHandlerParent : PointerEventHandler {

    private val handlers = mutableListOf<PointerEventHandler>()
    private var currentHandler: PointerEventHandler? = null

    fun addChildren(vararg handlersToAdd: PointerEventHandler) {
        addChildren(handlersToAdd.toList())
    }

    fun addChildren(handlersToAdd: Iterable<PointerEventHandler>) {
        handlersToAdd.forEach { handlers += it }
    }

    fun removeChildren(vararg handlersToRemove: PointerEventHandler) {
        removeChildren(handlersToRemove.toList())
    }

    private fun removeChildren(handlersToRemove: Iterable<PointerEventHandler>) {
        handlers.removeAll { handler ->
            handlersToRemove.any { handler === it }
        }
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
