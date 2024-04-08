package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.Command
import org.w3c.dom.HTMLCanvasElement

open class ShieldModulationUi(
    canvas: HTMLCanvasElement,
) : ModulationUi(
    canvas = canvas,
    xExpr = { 2.vmin },
    yExpr = { height - 14.vmin },
    widthExpr = { 46.vmin },
    decreaseCommand = Command.CommandDecreaseShieldModulation,
    increaseCommand = Command.CommandIncreaseShieldModulation
)
