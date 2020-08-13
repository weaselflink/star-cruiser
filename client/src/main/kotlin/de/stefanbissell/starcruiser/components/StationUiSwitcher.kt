package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.Station

class StationUiSwitcher(
    vararg stationList: StationUi
) {

    private val stations: List<StationUi> = stationList.toList()

    init {
        stations.forEach { it.hide() }
    }

    fun switchTo(station: Station?) {
        stations.forEach {
            if (it.station == station) {
                it.show()
            } else {
                it.hide()
            }
        }
    }
}

interface StationUi {
    val station: Station
    fun show()
    fun hide()
}
