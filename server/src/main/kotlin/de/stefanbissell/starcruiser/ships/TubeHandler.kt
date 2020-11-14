package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime

class TubeHandler(
    val launchTube: LaunchTube,
    val ship: Ship
) {

    var status: TubeStatus = TubeStatus.Empty

    fun update(
        time: GameTime,
        boostLevel: Double = 1.0
    ) {
        val currentStatus = status
        if (currentStatus is TubeStatus.Reloading) {
            val currentProgress = time.delta * launchTube.reloadSpeed * boostLevel
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
        }
    }

    fun startReload() {
        if (status is TubeStatus.Empty) {
            status = TubeStatus.Reloading()
        }
    }
}

sealed class TubeStatus {

    object Empty : TubeStatus()

    data class Reloading(val progress: Double = 0.0) : TubeStatus() {
        fun update(change: Double) = copy(progress = progress + change)
    }

    object Ready : TubeStatus()
}
