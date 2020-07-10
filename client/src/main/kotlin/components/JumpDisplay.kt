package components

import CanvasDimensions
import de.stefanbissell.starcruiser.JumpDriveMessage
import org.w3c.dom.HTMLCanvasElement

class JumpDisplay(
    canvas: HTMLCanvasElement,
    xExpr: (CanvasDimensions) -> Double,
    yExpr: (CanvasDimensions) -> Double,
    widthExpr: (CanvasDimensions) -> Double = { it.vmin * 34 },
    heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    private val canvasProgress = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = widthExpr,
        heightExpr = heightExpr,
        backgroundColor = "#111",
        foregroundColor = "#888"
    )

    fun draw(jumpDriveMessage: JumpDriveMessage) {
        when (jumpDriveMessage) {
            is JumpDriveMessage.Ready -> {
                canvasProgress.leftText = "Ready"
                canvasProgress.progress = null
            }
            is JumpDriveMessage.Jumping -> {
                canvasProgress.leftText = "Jumping"
                canvasProgress.progress = jumpDriveMessage.progress
            }
            is JumpDriveMessage.Recharging -> {
                canvasProgress.leftText = "Charging"
                canvasProgress.progress = jumpDriveMessage.progress
            }
        }

        canvasProgress.rightText = jumpDriveMessage.distance.toString()

        canvasProgress.draw()
    }
}
