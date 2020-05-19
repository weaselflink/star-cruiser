import de.bissell.starcruiser.clip
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.min

class CanvasSlider(
    private val xExpr: (Double) -> Double,
    private val yExpr: (Double) -> Double,
    private val widthExpr: (Double) -> Double,
    private val heightExpr: (Double) -> Double,
    private val lines: List<Double> = emptyList(),
    private val isHorizontal: Boolean = widthExpr(1.0) > heightExpr(1.0)
) {

    fun draw(canvas: HTMLCanvasElement, value: Double) {
        val ctx = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            lineWidth = 3.0
            fillStyle = "#111"
            beginPath()
            drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
            fill()

            strokeStyle = "#888"
            beginPath()
            drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
            stroke()

            fillStyle = "#999"
            beginPath()
            if (isHorizontal) {
                circle(
                    dim.bottomX + dim.radius + value.clip(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                    dim.bottomY - dim.radius,
                    dim.radius * 0.8
                )
            } else {
                circle(
                    dim.bottomX + dim.radius,
                    dim.bottomY - dim.radius - value.clip(0.0, 1.0) * (dim.length - dim.radius * 2.0),
                    dim.radius * 0.8
                )
            }
            fill()

            strokeStyle = "#666"
            lines.forEach {
                beginPath()
                if (isHorizontal) {
                    moveTo(dim.bottomX + dim.radius + it  * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 0.4)
                    lineTo(dim.bottomX + dim.radius + it  * (dim.length - dim.radius * 2.0), dim.bottomY - dim.radius * 1.6)
                } else {
                    moveTo(dim.bottomX + dim.radius * 0.4, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
                    lineTo(dim.bottomX + dim.radius * 1.6, dim.bottomY - dim.radius - it * (dim.length - dim.radius * 2.0))
                }
                stroke()
            }

            restore()
        }
    }

    fun isClickInside(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Boolean {
        val dim = currentDimensions(canvas)

        return mouseEvent.offsetX > dim.bottomX && mouseEvent.offsetX < dim.bottomX + dim.width
                && mouseEvent.offsetY > dim.bottomY - dim.height && mouseEvent.offsetY < dim.bottomY
    }

    fun clickValue(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Double {
        val dim = currentDimensions(canvas)

        return if (isHorizontal) {
            (mouseEvent.offsetX - (dim.bottomX + dim.radius)) / (dim.width - dim.radius * 2.0)
        } else {
            -(mouseEvent.offsetY - (dim.bottomY - dim.radius)) / (dim.height - dim.radius * 2.0)
        }.clip(0.0, 1.0)
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        min(canvas.width, canvas.height).toDouble().let { dim ->
            val width = widthExpr(dim)
            val height = heightExpr(dim)
            SliderDimensions(
                bottomX = xExpr(dim),
                bottomY = yExpr(dim),
                width = width,
                height = height,
                radius = if (isHorizontal) height * 0.5 else width * 0.5,
                length = if (isHorizontal) width else height
            )
        }
}

private data class SliderDimensions(
    val bottomX: Double,
    val bottomY: Double,
    val width: Double,
    val height: Double,
    val radius: Double,
    val length: Double
)
