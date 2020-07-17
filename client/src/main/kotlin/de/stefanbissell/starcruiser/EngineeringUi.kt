package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CapacitorsDisplay
import de.stefanbissell.starcruiser.components.PowerDisplay
import de.stefanbissell.starcruiser.components.RepairDisplay
import org.w3c.dom.CanvasRenderingContext2D

class EngineeringUi : CanvasUi(Station.Engineering, "engineering-ui") {

    private val capacitorsDisplay = CapacitorsDisplay(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 30 },
        yExpr = { it.vmin * 12 }
    )
    private val repairDisplay = RepairDisplay(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 30 },
        yExpr = { it.vmin * 20 }
    )
    private val powerDisplays = PoweredSystemType.values().mapIndexed { index, systemType ->
        systemType to PowerDisplay(
            systemType = systemType,
            canvas = canvas,
            yExpr = { it.height - it.vmin * 3 - it.vmin * index * 10 }
        )
    }.associate { it.first to it.second }

    init {
        powerDisplays.forEach {
            pointerEventDispatcher.addHandlers(it.value.handlers)
        }
    }

    fun draw(snapshot: SnapshotMessage.Engineering) {
        ctx.draw(snapshot.powerSettings)
    }

    private fun CanvasRenderingContext2D.draw(powerMessage: PowerMessage) {
        transformReset()
        clear("#222")

        capacitorsDisplay.draw(powerMessage)
        repairDisplay.draw(powerMessage)
        powerMessage.settings.forEach {
            powerDisplays[it.key]?.draw(powerMessage)
        }
    }
}
