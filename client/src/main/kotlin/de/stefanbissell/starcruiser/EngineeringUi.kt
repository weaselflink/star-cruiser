package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CapacitorsDisplay
import de.stefanbissell.starcruiser.components.PowerDisplay
import org.w3c.dom.CanvasRenderingContext2D

class EngineeringUi : CanvasUi(Station.Engineering, "engineering-ui") {

    private val capacitorsDisplay = CapacitorsDisplay(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 30 },
        yExpr = { it.vmin * 12 }
    )
    private val powerDisplays = PoweredSystem.values().mapIndexed { index, system ->
        system to PowerDisplay(
            system = system,
            canvas = canvas,
            yExpr = { it.height - it.vmin * 3 - it.vmin * index * 12 }
        )
    }.associate { it.first to it.second }

    init {
        powerDisplays.forEach { pointerEventDispatcher.addHandlers(it.value) }
    }

    fun draw(snapshot: SnapshotMessage.Engineering) {
        ctx.draw(snapshot.powerSettings)
    }

    private fun CanvasRenderingContext2D.draw(powerMessage: PowerMessage) {
        transformReset()
        clear("#222")

        capacitorsDisplay.draw(powerMessage)
        powerMessage.settings.forEach {
            powerDisplays[it.key]?.draw(powerMessage)
        }
    }
}
