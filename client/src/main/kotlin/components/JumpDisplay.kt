package components

import CanvasDimensions
import de.bissell.starcruiser.JumpDriveMessage
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.MIDDLE
import org.w3c.dom.RIGHT
import px

class JumpDisplay(
    private val canvas: HTMLCanvasElement,
    private val ctx: CanvasRenderingContext2D = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 34 },
    private val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    fun draw(jumpDriveMessage: JumpDriveMessage) {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            lineWidth = dim.lineWidth * 0.5
            val textSize = (dim.height * 0.7).toInt()
            font = "${textSize.px} sans-serif"

            drawBackground(dim)
            drawBorder(dim)
            drawStatusText(dim, jumpDriveMessage)
            drawDistanceText(dim, jumpDriveMessage)

            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawBackground(dim: ComponentDimensions) {
        fillStyle = "#111"
        fillRect(dim.bottomX, dim.bottomY, dim.width, -dim.height)
    }

    private fun CanvasRenderingContext2D.drawBorder(dim: ComponentDimensions) {
        strokeStyle = "#888"
        strokeRect(dim.bottomX, dim.bottomY, dim.width, -dim.height)
    }

    private fun CanvasRenderingContext2D.drawStatusText(dim: ComponentDimensions, jumpDriveMessage: JumpDriveMessage) {
        val margin = marginExpr(dim)
        val statusText = when (jumpDriveMessage) {
            is JumpDriveMessage.Ready -> "Ready"
            is JumpDriveMessage.Jumping -> "Jumping"
            is JumpDriveMessage.Recharging -> "Charging"
        }

        fillStyle = "#888"
        textAlign = CanvasTextAlign.LEFT
        textBaseline = CanvasTextBaseline.MIDDLE
        fillText(statusText, dim.bottomX + margin, dim.bottomY - dim.height * 0.5)
    }

    private fun CanvasRenderingContext2D.drawDistanceText(dim: ComponentDimensions, jumpDriveMessage: JumpDriveMessage) {
        val margin = marginExpr(dim)
        val distanceText = jumpDriveMessage.distance.toString()

        fillStyle = "#888"
        textAlign = CanvasTextAlign.RIGHT
        textBaseline = CanvasTextBaseline.MIDDLE
        fillText(distanceText, dim.bottomX + dim.width - margin, dim.bottomY - dim.height * 0.5)
    }

    private fun marginExpr(dim: ComponentDimensions) = dim.height * 0.2

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}