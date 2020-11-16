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
        if (tubesMessage.tubes.size != tubeDisplays.size) {
            tubeDisplays.forEach {
                removeChildren(it)
            }

            tubeDisplays = tubesMessage.tubes
                .reversed()
                .mapIndexed { index, _ ->
                    LaunchTubeDisplay(
                        canvas = canvas,
                        xExpr = xExpr,
                        yExpr = { yExpr() - 10.vmin * index },
                        index = index
                    ).also {
                        addChildren(it)
                    }
                }
        }
        tubesMessage.tubes
            .reversed()
            .forEachIndexed { index, tubeStatus ->
                tubeDisplays[index].draw(tubeStatus)
            }
    }
}
