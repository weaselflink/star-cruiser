package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ClientSocket
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.PowerMessage
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
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
        xExpr = { xOffset() + 3.vmin },
        yExpr = yExpr,
        widthExpr = { 8.vmin },
        heightExpr = { 8.vmin },
        onClick = { ClientSocket.send(Command.CommandStartRepair(systemType)) },
        activated = { repairing },
        initialText = "\ud83d\udee0"
    )
    private val levelSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { xOffset() + 13.vmin },
        yExpr = yExpr,
        widthExpr = { 41.vmin },
        heightExpr = { 8.vmin },
        onChange = {
            val power = (it * 200).roundToInt()
            ClientSocket.send(Command.CommandSetPower(systemType, power))
        },
        lines = listOf(0.5),
        leftText = systemType.name
    )
    private val heat = CanvasProgress(
        canvas = canvas,
        xExpr = { xOffset() + 56.vmin },
        yExpr = { yExpr() - 4.5.vmin },
        widthExpr = { 14.vmin },
        heightExpr = { 3.5.vmin },
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
        xExpr = { xOffset() + 56.vmin },
        yExpr = yExpr,
        widthExpr = { 14.vmin },
        heightExpr = { 3.5.vmin },
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
        xExpr = { xOffset() + 72.vmin },
        yExpr = yExpr,
        widthExpr = { 25.vmin },
        heightExpr = { 8.vmin },
        onChange = {
            ClientSocket.send(Command.CommandSetCoolant(systemType, it))
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
