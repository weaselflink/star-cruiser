package de.stefanbissell.starcruiser.ships.combat

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TubeStatus
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.Tube

class TubeHandler(
    val tube: Tube,
    val ship: Ship
) {

    var status: TubeStatus = TubeStatus.Empty
    var newTorpedo = false

    fun update(
        time: GameTime,
        boostLevel: Double = 1.0
    ) {
        newTorpedo = false
        when (val currentStatus = status) {
            is TubeStatus.Launching -> {
                status = TubeStatus.Empty
                newTorpedo = true
            }
            is TubeStatus.Reloading -> {
                val currentProgress = time.delta * tube.reloadSpeed * boostLevel
                status = if (currentStatus.progress + currentProgress < 1.0) {
                    currentStatus.update(currentProgress)
                } else {
                    TubeStatus.Ready
                }
            }
            else -> {}
        }
    }

    fun requestLaunch() {
        if (status is TubeStatus.Ready) {
            status = TubeStatus.Launching
        }
    }

    fun requestReload(): Boolean {
        return if (status is TubeStatus.Empty) {
            status = TubeStatus.Reloading()
            true
        } else {
            false
        }
    }
}
