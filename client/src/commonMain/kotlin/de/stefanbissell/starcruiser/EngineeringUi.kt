package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CapacitorsDisplay
import de.stefanbissell.starcruiser.components.PowerDisplay
import de.stefanbissell.starcruiser.components.RepairDisplay
import de.stefanbissell.starcruiser.components.StationUi
import org.w3c.dom.CanvasRenderingContext2D

class EngineeringUi : StationUi(Station.Engineering) {

    private val capacitorsDisplay = CapacitorsDisplay(canvas)
    private val repairDisplay = RepairDisplay(canvas)
    private val powerDisplays = PoweredSystemType.entries.mapIndexed { index, systemType ->
        systemType to PowerDisplay(
            systemType = systemType,
            canvas = canvas,
            yExpr = { height - 3.vmin - 10.vmin * index }
        )
    }.associate { it.first to it.second }

    init {
        addChildren(repairDisplay)
        addChildren(powerDisplays.values)
    }

    fun draw(snapshot: SnapshotMessage.Engineering) {
        ctx.draw(snapshot.powerSettings)
    }

    private fun CanvasRenderingContext2D.draw(powerMessage: PowerMessage) {
        transformReset()
        clearBackground()

        capacitorsDisplay.draw(powerMessage)
        powerMessage.settings.forEach {
            powerDisplays[it.key]?.draw(powerMessage)
        }
        repairDisplay.draw(powerMessage)
    }
}
