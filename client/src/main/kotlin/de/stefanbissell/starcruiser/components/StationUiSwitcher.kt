package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.EngineeringUi
import de.stefanbissell.starcruiser.HelmUi
import de.stefanbissell.starcruiser.MainScreenUi
import de.stefanbissell.starcruiser.NavigationUi
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Station
import de.stefanbissell.starcruiser.WeaponsUi
import de.stefanbissell.starcruiser.canvas2d
import de.stefanbissell.starcruiser.clear
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventDispatcher
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.updateSize
import kotlinx.browser.document

class StationUiSwitcher {

    private val stationOverlay = StationOverlay()
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
        mainScreenUi
    )
    private val canvas = document.canvas2d
    private val pointerEventDispatcher = PointerEventDispatcher(canvas)

    init {
        pointerEventDispatcher.addHandlers(stationOverlay)
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
        switchTo(null)
    }

    fun draw(snapshot: SnapshotMessage.CrewSnapshot) {
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
        stationOverlay.draw(snapshot)
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
            stationOverlay.visible = false
            canvas.context2D.clear()
        } else {
            stationOverlay.visible = true
        }
    }
}

abstract class StationUi(
    val station: Station
) : PointerEventHandlerParent() {

    var visible = false

    val canvas = document.canvas2d
    val ctx = canvas.context2D

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return visible && super.isInterestedIn(pointerEvent)
    }

    open fun resize() = Unit

    open fun show() = Unit

    open fun hide() = Unit
}
