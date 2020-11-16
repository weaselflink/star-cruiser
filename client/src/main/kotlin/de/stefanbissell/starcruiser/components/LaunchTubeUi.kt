package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.TubesMessage
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import org.w3c.dom.HTMLCanvasElement

class LaunchTubeUi(
    private val canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double,
    private val yExpr: CanvasDimensions.() -> Double,
) : PointerEventHandlerParent() {

    private var tubeDisplays = emptyList<LaunchTubeDisplay>()

    fun draw(tubesMessage: TubesMessage) {
        val size = tubesMessage.tubes.size
        if (size != tubeDisplays.size) {
            tubeDisplays.forEach {
                removeChildren(it)
            }

            tubeDisplays = tubesMessage.tubes
                .mapIndexed { index, _ ->
                    LaunchTubeDisplay(
                        canvas = canvas,
                        xExpr = xExpr,
                        yExpr = { yExpr() - 10.vmin * (size - (index + 1)) },
                        index = index
                    ).also {
                        addChildren(it)
                    }
                }
        }
        tubesMessage.tubes
            .forEachIndexed { index, tubeStatus ->
                tubeDisplays[index].draw(tubeStatus)
            }
    }
}
