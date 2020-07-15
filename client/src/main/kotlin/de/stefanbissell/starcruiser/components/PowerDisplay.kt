package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.send
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

class PowerDisplay(
    private val systemType: PoweredSystemType,
    canvas: HTMLCanvasElement,
    yExpr: (CanvasDimensions) -> Double
) {

    private val levelSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 3 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 44 },
        heightExpr = { it.vmin * 8 },
        onChange = {
            val power = (it * 200).roundToInt()
            clientSocket.send(Command.CommandSetPower(systemType, power))
        },
        lines = listOf(0.5),
        leftText = systemType.name
    )
    private val heat = CanvasProgress(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 50 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 17 },
        heightExpr = { it.vmin * 8 },
        backgroundColor = "#111",
        foregroundColor = "#888"
    )
    private val coolantSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 70 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 27 },
        heightExpr = { it.vmin * 8 },
        onChange = {
            clientSocket.send(Command.CommandSetCoolant(systemType, it))
        }
    )

    val handlers: List<PointerEventHandler>
        get() = listOf(levelSlider, coolantSlider)

    fun draw(powerMessage: PowerMessage) {
        val systemMessage = powerMessage.settings[systemType]
            ?: PoweredSystemMessage(
                level = 100,
                heat = 0.0,
                coolant = 0.0
            )
        val position = systemMessage.level.toDouble() / 200.0

        levelSlider.draw(position)

        heat.progress = systemMessage.heat
        heat.draw()

        coolantSlider.draw(systemMessage.coolant)
    }

    private fun CanvasDimensions.xOffset() = (width - min) * 0.5
}
