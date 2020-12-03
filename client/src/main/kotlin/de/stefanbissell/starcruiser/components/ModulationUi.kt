package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import org.w3c.dom.HTMLCanvasElement

open class ModulationUi(
    canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
    private val widthExpr: CanvasDimensions.() -> Double = { 46.vmin },
    decreaseCommand: Command,
    increaseCommand: Command
) : PointerEventHandlerParent() {

    private val spinner = CanvasSpinner(
        canvas = canvas,
        xExpr = { xExpr() },
        yExpr = { yExpr() },
        widthExpr = { widthExpr() },
        decreaseCommand = decreaseCommand,
        increaseCommand = increaseCommand
    )

    init {
        addChildren(spinner)
    }

    fun draw(modulation: Int) {
        val value = modulation * 2 + 78
        spinner.text = "$value PHz"

        spinner.draw()
    }
}
