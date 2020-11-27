package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ClientSocket
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.TubeStatus
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import org.w3c.dom.HTMLCanvasElement

class LaunchTubeDisplay(
    canvas: HTMLCanvasElement,
    xExpr: CanvasDimensions.() -> Double,
    yExpr: CanvasDimensions.() -> Double,
    private val index: Int
) : PointerEventHandlerParent() {

    private var currentStatus: TubeStatus = TubeStatus.Empty
    private val actionPossible: Boolean
        get() = currentStatus in listOf<TubeStatus>(TubeStatus.Empty, TubeStatus.Ready)

    private val statusBar = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = { yExpr() - 1.vmin },
        widthExpr = { 24.vmin },
        heightExpr = { 6.vmin }
    )
    private val actionButton = CanvasButton(
        canvas = canvas,
        xExpr = { xExpr() + 26.vmin },
        yExpr = yExpr,
        widthExpr = { 20.vmin },
        heightExpr = { 8.vmin },
        onClick = { onActionButton() },
        enabled = { actionPossible }
    )

    init {
        addChildren(actionButton)
    }

    fun draw(status: TubeStatus) {
        currentStatus = status
        drawStatus()
        drawButton()
    }

    private fun drawStatus() {
        when (val status = currentStatus) {
            is TubeStatus.Empty -> {
                statusBar.leftText = "Empty"
                statusBar.progress = 0.0
            }
            is TubeStatus.Reloading -> {
                statusBar.leftText = "Reloading"
                statusBar.progress = status.progress
            }
            is TubeStatus.Ready -> {
                statusBar.leftText = "Ready"
                statusBar.progress = 1.0
            }
            is TubeStatus.Launching -> {
                statusBar.leftText = "Launching"
                statusBar.progress = 1.0
            }
        }

        statusBar.draw()
    }

    private fun drawButton() {
        when (currentStatus) {
            is TubeStatus.Empty -> {
                actionButton.text = "Load"
            }
            is TubeStatus.Reloading -> {
                actionButton.text = "Load"
            }
            is TubeStatus.Ready -> {
                actionButton.text = "Launch"
            }
            is TubeStatus.Launching -> {
                statusBar.leftText = "Launch"
            }
        }

        actionButton.draw()
    }

    private fun onActionButton() {
        when (currentStatus) {
            is TubeStatus.Empty -> ClientSocket.send(Command.CommandReloadTube(index))
            is TubeStatus.Ready -> ClientSocket.send(Command.CommandLaunchTube(index))
            else -> Unit
        }
    }
}
