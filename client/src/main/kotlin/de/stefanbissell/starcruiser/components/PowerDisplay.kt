package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.send
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

class PowerDisplay(
    private val systemType: PoweredSystemType,
    canvas: HTMLCanvasElement,
    yExpr: (CanvasDimensions) -> Double
) {

    private val repairButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 3 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 8 },
        heightExpr = { it.vmin * 8 },
        onClick = { clientSocket.send(Command.CommandRepair(systemType)) },
        activated = { repairing },
        text = { "\ud83d\udee0" }
    )
    private val levelSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 13 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 41 },
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
        xExpr = { it.xOffset() + it.vmin * 56 },
        yExpr = { yExpr(it) - it.vmin * 4.5 },
        widthExpr = { it.vmin * 14 },
        heightExpr = { it.vmin * 3.5 },
        backgroundColor = "#111",
        foregroundColor = "#888"
    )
    private val damage = CanvasProgress(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 56 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 14 },
        heightExpr = { it.vmin * 3.5 },
        backgroundColor = "#111",
        foregroundColor = "#888"
    )
    private val coolantSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.xOffset() + it.vmin * 72 },
        yExpr = yExpr,
        widthExpr = { it.vmin * 25 },
        heightExpr = { it.vmin * 8 },
        onChange = {
            clientSocket.send(Command.CommandSetCoolant(systemType, it))
        }
    )

    val handlers: List<PointerEventHandler>
        get() = listOf(repairButton, levelSlider, coolantSlider)

    private var repairing = false

    fun draw(powerMessage: PowerMessage) {
        val systemMessage = powerMessage.settings[systemType]
            ?: PoweredSystemMessage(
                repairProgress = null,
                damage = 0.0,
                level = 100,
                heat = 0.0,
                coolant = 0.0
            )
        repairing = systemMessage.repairProgress != null
        val position = systemMessage.level.toDouble() / 200.0

        repairButton.draw()

        levelSlider.draw(position)

        heat.progress = systemMessage.heat
        heat.draw()
        damage.progress = 1.0 - systemMessage.damage
        damage.centerText = "${damage.progress.toPercent()}%"
        damage.draw()

        coolantSlider.draw(systemMessage.coolant)
    }

    private fun CanvasDimensions.xOffset() = (width - min) * 0.5
}
