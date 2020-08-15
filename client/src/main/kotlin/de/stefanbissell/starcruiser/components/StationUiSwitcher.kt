package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.Station
import de.stefanbissell.starcruiser.canvas
import de.stefanbissell.starcruiser.clear
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventDispatcher
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.updateSize
import kotlinx.browser.document

class StationUiSwitcher(
    vararg stationList: StationUi
) {

    private val stations: List<StationUi> = stationList.toList()
    private val canvas = document.body!!.canvas
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)

    init {
        println(document.body!!.canvas.tagName)
        stations.forEach {
            it.visible = false
            it.hide()
            pointerEventDispatcher.addHandlers(it)
        }
        resize()
    }

    fun resize() {
        canvas.updateSize()
        stations.forEach {
            it.resize()
        }
    }

    fun switchTo(station: Station?) {
        stations.forEach {
            if (it.station == station) {
                it.visible = true
                it.show()
            } else {
                it.visible = false
                it.hide()
            }
        }
        if (stations.none { it.visible }) {
            canvas.context2D.clear()
        }
    }
}

abstract class StationUi : PointerEventHandlerParent() {

    abstract val station: Station

    var visible = false

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return visible && super.isInterestedIn(pointerEvent)
    }

    open fun resize() = Unit

    open fun show() = Unit

    open fun hide() = Unit
}
