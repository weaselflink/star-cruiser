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

    private val magazineDisplay = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = { yExpr() - 10.vmin * tubeDisplays.size },
        widthExpr = { 46.vmin }
    ).also {
        it.leftText = "Magazine"
    }
    private var tubeDisplays = emptyList<LaunchTubeDisplay>()

    fun draw(tubesMessage: TubesMessage) {
        updateTubeDisplays(tubesMessage)
        drawMagazine(tubesMessage)
        drawTubeDisplays(tubesMessage)
    }

    private fun updateTubeDisplays(tubesMessage: TubesMessage) {
        val size = tubesMessage.tubes.size
        if (size != tubeDisplays.size) {
            tubeDisplays.forEach {
                removeChildren(it)
            }

            tubeDisplays = tubesMessage.tubes
                .mapIndexed { index, _ ->
                    createTubeDisplay(size, index)
                }
        }
    }

    private fun createTubeDisplay(size: Int, index: Int) =
        LaunchTubeDisplay(
            canvas = canvas,
            xExpr = xExpr,
            yExpr = { yExpr() - 10.vmin * (size - (index + 1)) },
            index = index
        ).also {
            addChildren(it)
        }

    private fun drawTubeDisplays(tubesMessage: TubesMessage) {
        tubesMessage.tubes
            .forEachIndexed { index, tubeStatus ->
                tubeDisplays[index].draw(tubeStatus)
            }
    }

    private fun drawMagazine(tubesMessage: TubesMessage) {
        val remaining = tubesMessage.magazineRemaining
        val max = tubesMessage.magazineMax
        magazineDisplay.progress = remaining.toDouble() / max.toDouble()
        magazineDisplay.rightText = "$remaining/$max"
        magazineDisplay.draw()
    }
}
