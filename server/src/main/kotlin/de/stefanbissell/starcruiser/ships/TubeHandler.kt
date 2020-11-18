package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.TubeStatus

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
        val currentStatus = status
        if (currentStatus is TubeStatus.Reloading) {
            val currentProgress = time.delta * tube.reloadSpeed * boostLevel
            status = if (currentStatus.progress + currentProgress < 1.0) {
                currentStatus.update(currentProgress)
            } else {
                TubeStatus.Ready
            }
        }
    }

    fun launch() {
        if (status is TubeStatus.Ready) {
            status = TubeStatus.Empty
            newTorpedo = true
        }
    }

    fun startReload(): Boolean {
        return if (status is TubeStatus.Empty) {
            status = TubeStatus.Reloading()
            true
        } else {
            false
        }
    }

    fun endUpdate() =
        newTorpedo.also {
            newTorpedo = false
        }
}
