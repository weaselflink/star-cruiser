package components

import de.bissell.starcruiser.Station

class StationUiSwitcher(
    private val stations: List<StationUi>
) {

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
