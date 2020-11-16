package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.TubesMessage
import org.w3c.dom.HTMLCanvasElement

class LaunchTubeUi(
    private val canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
) {

    fun draw(tubesMessage: TubesMessage) {
        tubesMessage.tubes
            .reversed()
            .forEachIndexed { index, tubeStatus ->
                LaunchTubeDisplay(
                    canvas = canvas,
                    xExpr = xExpr,
                    yExpr = { yExpr() - 10.vmin * index },
                    index = index
                ).draw(tubeStatus)
            }
    }
}
