package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.Command
import org.w3c.dom.HTMLCanvasElement

open class BeamModulationUi(
    canvas: HTMLCanvasElement,
) : ModulationUi(
    canvas = canvas,
    xExpr = { 2.vmin },
    yExpr = { height - 45.vmin },
    widthExpr = { 46.vmin },
    decreaseCommand = Command.CommandDecreaseBeamModulation,
    increaseCommand = Command.CommandIncreaseBeamModulation
)
