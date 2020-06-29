package components

import CanvasDimensions
import context2D
import de.bissell.starcruiser.JumpDriveMessage
import org.w3c.dom.*
import px

class JumpDisplay(
    private val canvas: HTMLCanvasElement,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 34 },
    private val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    private val ctx: CanvasRenderingContext2D = canvas.context2D

    fun draw(jumpDriveMessage: JumpDriveMessage) {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            lineWidth = dim.lineWidth * 0.5
            val textSize = (dim.height * 0.7).toInt()
            font = "${textSize.px} sans-serif"

            drawBackground(dim)
            drawBorder(dim)
            fillStyle = "#888"
            textBaseline = CanvasTextBaseline.MIDDLE
            drawStatusText(dim, jumpDriveMessage)
            drawDistanceText(dim, jumpDriveMessage)
            barWidth(jumpDriveMessage)?.also {
                drawBar(dim, it)
                clipBar(dim, it)
                fillStyle = "#111"
                drawStatusText(dim, jumpDriveMessage)
                drawDistanceText(dim, jumpDriveMessage)
            }


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

        textAlign = CanvasTextAlign.LEFT
        fillText(statusText, dim.bottomX + margin, dim.bottomY - dim.height * 0.5)
    }

    private fun CanvasRenderingContext2D.drawDistanceText(dim: ComponentDimensions, jumpDriveMessage: JumpDriveMessage) {
        val margin = marginExpr(dim)
        val distanceText = jumpDriveMessage.distance.toString()

        textAlign = CanvasTextAlign.RIGHT
        fillText(distanceText, dim.bottomX + dim.width - margin, dim.bottomY - dim.height * 0.5)
    }

    private fun CanvasRenderingContext2D.drawBar(dim: ComponentDimensions, barWidth: Double) {
        fillStyle = "#888"
        drawBarRect(dim, barWidth)
        fill()
    }

    private fun CanvasRenderingContext2D.clipBar(dim: ComponentDimensions, barWidth: Double) {
        drawBarRect(dim, barWidth)
        clip()
    }

    private fun CanvasRenderingContext2D.drawBarRect(dim: ComponentDimensions, barWidth: Double) {
        beginPath()
        rect(dim.bottomX, dim.bottomY, dim.width * barWidth, -dim.height)
    }

    private fun barWidth(jumpDriveMessage: JumpDriveMessage) =
        when (jumpDriveMessage) {
            is JumpDriveMessage.Jumping -> jumpDriveMessage.progress
            is JumpDriveMessage.Recharging -> jumpDriveMessage.progress
            else -> null
        }

    private fun marginExpr(dim: ComponentDimensions) = dim.height * 0.2

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}
