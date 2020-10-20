package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.clientSocket
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

class PowerDisplay(
    private val systemType: PoweredSystemType,
    canvas: HTMLCanvasElement,
    yExpr: CanvasDimensions.() -> Double
) : PointerEventHandlerParent() {

    private val repairButton = CanvasButton(
        canvas = canvas,
        xExpr = { xOffset() + vmin * 3 },
        yExpr = yExpr,
        widthExpr = { vmin * 8 },
        heightExpr = { vmin * 8 },
        onClick = { clientSocket.send(Command.CommandStartRepair(systemType)) },
        activated = { repairing },
        initialText = "\ud83d\udee0"
    )
    private val levelSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { xOffset() + vmin * 13 },
        yExpr = yExpr,
        widthExpr = { vmin * 41 },
        heightExpr = { vmin * 8 },
        onChange = {
            val power = (it * 200).roundToInt()
            clientSocket.send(Command.CommandSetPower(systemType, power))
        },
        lines = listOf(0.5),
        leftText = systemType.name
    )
    private val heat = CanvasProgress(
        canvas = canvas,
        xExpr = { xOffset() + vmin * 56 },
        yExpr = { yExpr() - vmin * 4.5 },
        widthExpr = { vmin * 14 },
        heightExpr = { vmin * 3.5 },
        foregroundColorExpr = {
            when {
                it < 0.5 -> UiStyle.buttonForegroundColor
                it < 0.75 -> UiStyle.warningYellow
                else -> UiStyle.warningRed
            }
        }
    )
    private val damage = CanvasProgress(
        canvas = canvas,
        xExpr = { xOffset() + vmin * 56 },
        yExpr = yExpr,
        widthExpr = { vmin * 14 },
        heightExpr = { vmin * 3.5 },
        foregroundColorExpr = {
            when {
                it > 0.9 -> UiStyle.buttonForegroundColor
                it > 0.5 -> UiStyle.warningYellow
                else -> UiStyle.warningRed
            }
        }
    )
    private val coolantSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { xOffset() + vmin * 72 },
        yExpr = yExpr,
        widthExpr = { vmin * 25 },
        heightExpr = { vmin * 8 },
        onChange = {
            clientSocket.send(Command.CommandSetCoolant(systemType, it))
        }
    )

    init {
        addChildren(repairButton, levelSlider, coolantSlider)
    }

    private var repairing = false

    fun draw(powerMessage: PowerMessage) {
        val systemMessage = powerMessage.settings[systemType]
            ?: PoweredSystemMessage(
                damage = 0.0,
                level = 100,
                heat = 0.0,
                coolant = 0.0
            )
        repairing = powerMessage.repairProgress?.type == systemType
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
