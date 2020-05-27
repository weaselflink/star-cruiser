import de.bissell.starcruiser.clamp
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.min

class CanvasSlider(
    private val xExpr: (CurrentCanvasSize) -> Double,
    private val yExpr: (CurrentCanvasSize) -> Double,
    private val widthExpr: (CurrentCanvasSize) -> Double,
    private val heightExpr: (CurrentCanvasSize) -> Double,
    private val onChange: (Double) -> Unit = {},
    private val lines: List<Double> = emptyList()
) : MouseEventHandler {

    fun draw(canvas: HTMLCanvasElement, value: Double) {
        val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawPill(dim)
            drawKnob(dim, value)
            drawLines(dim)

            restore()
        }
    }

    override fun isInterestedIn(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Boolean {
        val dim = currentDimensions(canvas)

        return mouseEvent.offsetX > dim.bottomX && mouseEvent.offsetX < dim.bottomX + dim.width
                && mouseEvent.offsetY > dim.bottomY - dim.height && mouseEvent.offsetY < dim.bottomY
    }

    override fun handleMouseDown(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        onChange(clickValue(canvas, mouseEvent))
    }

    override fun handleMouseMove(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        onChange(clickValue(canvas, mouseEvent))
    }

    override fun handleMouseUp(canvas: HTMLCanvasElement, mouseEvent: MouseEvent) {
        onChange(clickValue(canvas, mouseEvent))
    }

    private fun clickValue(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Double {
        val dim = currentDimensions(canvas)

        return if (dim.isHorizontal) {
            (mouseEvent.offsetX - (dim.bottomX + dim.radius)) / (dim.width - dim.radius * 2.0)
        } else {
            -(mouseEvent.offsetY - (dim.bottomY - dim.radius)) / (dim.height - dim.radius * 2.0)
        }.clamp(0.0, 1.0)
    }

    private fun CanvasRenderingContext2D.drawPill(dim: SliderDimensions) {
        lineWidth = dim.lineWidth
        fillStyle = "#111"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        fill()

        strokeStyle = "#888"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawKnob(dim: SliderDimensions, value: Double) {
        fillStyle = "#999"
        beginPath()
        if (dim.isHorizontal) {
            circle(
                dim.bottomX + dim.radius + value.clamp(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                dim.bottomY - dim.radius,
                dim.radius * 0.8
            )
        } else {
            circle(
                dim.bottomX + dim.radius,
                dim.bottomY - dim.radius - value.clamp(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                dim.radius * 0.8
            )
        }
        fill()
    }

    private fun CanvasRenderingContext2D.drawLines(dim: SliderDimensions) {
        strokeStyle = "#666"
        lines.forEach {
            beginPath()
            if (dim.isHorizontal) {
                moveTo(dim.bottomX + dim.radius + it * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 0.4)
                lineTo(dim.bottomX + dim.radius + it * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 1.6)
            } else {
                moveTo(dim.bottomX + dim.radius * 0.4, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
                lineTo(dim.bottomX + dim.radius * 1.6, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
            }
            stroke()
        }
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        CurrentCanvasSize(canvas.width.toDouble(), canvas.height.toDouble()).let { dim ->
            val width = widthExpr(dim)
            val height = heightExpr(dim)
            SliderDimensions(
                bottomX = xExpr(dim),
                bottomY = yExpr(dim),
                width = width,
                height = height,
                radius = if (width > height) height * 0.5 else width * 0.5,
                length = if (width > height) width else height,
                lineWidth = dim.dim * 0.004
            )
        }
}

data class CurrentCanvasSize(
    val width: Double,
    val height: Double,
    val dim: Double = min(width, height)
)

private data class SliderDimensions(
    val bottomX: Double,
    val bottomY: Double,
    val width: Double,
    val height: Double,
    val radius: Double,
    val length: Double,
    val lineWidth: Double,
    val isHorizontal: Boolean = width > height
)
