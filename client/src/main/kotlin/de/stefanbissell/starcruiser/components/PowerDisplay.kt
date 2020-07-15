package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystem
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.send
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

class PowerDisplay(
    private val system: PoweredSystem,
    canvas: HTMLCanvasElement,
    yExpr: (CanvasDimensions) -> Double
) : PointerEventHandler {

    private val slider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 60 },
        heightExpr = { it.vmin * 10 },
        onChange = {
            val power = (it * 200).roundToInt()
            clientSocket.send(Command.CommandSetPower(system, power))
        },
        lines = listOf(0.5),
        leftText = system.name
    )
    private val heat = CanvasProgress(
        canvas = canvas,
        xExpr = { it.vmin * 66 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 40 },
        heightExpr = { it.vmin * 10 },
        backgroundColor = "#111",
        foregroundColor = "#888"
    )

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return slider.isInterestedIn(pointerEvent)
    }

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        slider.handlePointerDown(pointerEvent)
    }

    override fun handlePointerMove(pointerEvent: PointerEvent) {
        slider.handlePointerMove(pointerEvent)
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        slider.handlePointerUp(pointerEvent)
    }

    fun draw(powerMessage: PowerMessage) {
        val systemMessage = powerMessage.settings[system]
            ?: PoweredSystemMessage(
                level = 100,
                heat = 0.0
            )
        val position = systemMessage.level.toDouble() / 200.0

        slider.draw(position)

        heat.progress = systemMessage.heat
        heat.draw()
    }
}
