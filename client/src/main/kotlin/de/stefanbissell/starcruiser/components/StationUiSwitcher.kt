package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CommonShipUi
import de.stefanbissell.starcruiser.EngineeringUi
import de.stefanbissell.starcruiser.HelmUi
import de.stefanbissell.starcruiser.MainScreenUi
import de.stefanbissell.starcruiser.NavigationUi
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Station
import de.stefanbissell.starcruiser.WeaponsUi
import de.stefanbissell.starcruiser.clear
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventDispatcher
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.updateSize
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

class StationUiSwitcher {

    private val commonShipUi = CommonShipUi().apply {
        hide()
    }
    private val helmUi = HelmUi()
    private val weaponsUi = WeaponsUi()
    private val navigationUi = NavigationUi()
    private val engineeringUi = EngineeringUi()
    private val mainScreenUi = MainScreenUi()
    private val stations: List<StationUi> = listOf(
        helmUi,
        weaponsUi,
        navigationUi,
        engineeringUi,
        mainScreenUi,
    )
    private val canvas = document.body!!.querySelector(".canvas2d") as HTMLCanvasElement
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)

    init {
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

    fun hideAll() {
        commonShipUi.hide()
        switchTo(null)
    }

    fun draw(snapshot: SnapshotMessage.CrewSnapshot) {
        commonShipUi.apply {
            show()
            draw(snapshot)
        }
        when (snapshot) {
            is SnapshotMessage.Helm -> {
                switchTo(Station.Helm)
                helmUi.draw(snapshot)
            }
            is SnapshotMessage.Weapons -> {
                switchTo(Station.Weapons)
                weaponsUi.draw(snapshot)
            }
            is SnapshotMessage.Navigation -> {
                switchTo(Station.Navigation)
                navigationUi.draw(snapshot)
            }
            is SnapshotMessage.Engineering -> {
                switchTo(Station.Engineering)
                engineeringUi.draw(snapshot)
            }
            is SnapshotMessage.MainScreen -> {
                switchTo(Station.MainScreen)
                mainScreenUi.draw(snapshot)
            }
        }
    }

    private fun switchTo(station: Station?) {
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

abstract class StationUi(
    val station: Station
) : PointerEventHandlerParent() {

    var visible = false

    val canvas = document.body!!.querySelector(".canvas2d") as HTMLCanvasElement
    val ctx = canvas.context2D

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return visible && super.isInterestedIn(pointerEvent)
    }

    open fun resize() = Unit

    open fun show() = Unit

    open fun hide() = Unit
}
