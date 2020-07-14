package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CanvasSlider
import de.stefanbissell.starcruiser.components.CapacitorsDisplay
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.roundToInt

class EngineeringUi : CanvasUi(Station.Engineering, "engineering-ui") {

    private val capacitorsDisplay = CapacitorsDisplay(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 30 },
        yExpr = { it.vmin * 12 }
    )
    private val sliders = PoweredSystem.values().mapIndexed { index, system ->
        system to CanvasSlider(
            canvas = canvas,
            xExpr = { it.vmin * 3 },
            yExpr = { it.height - it.vmin * 3 - it.vmin * index * 12 },
            widthExpr = { it.vmin * 60 },
            heightExpr = { it.vmin * 10 },
            onChange = {
                val power = (it * 200).roundToInt()
                clientSocket.send(Command.CommandSetPower(system, power))
            },
            lines = listOf(0.5),
            leftText = system.name
        )
    }.associate { it.first to it.second }

    init {
        sliders.forEach { pointerEventDispatcher.addHandlers(it.value) }
    }

    fun draw(snapshot: SnapshotMessage.Engineering) {
        ctx.draw(snapshot.powerSettings)
    }

    private fun CanvasRenderingContext2D.draw(powerSettings: PowerMessage) {
        transformReset()
        clear("#222")

        capacitorsDisplay.draw(powerSettings)
        powerSettings.settings.forEach {
            val position = it.value.toDouble() / 200
            sliders[it.key]?.draw(position)
        }
    }
}
