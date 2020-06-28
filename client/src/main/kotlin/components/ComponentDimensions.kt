package components

import CanvasDimensions
import dimensions
import org.w3c.dom.HTMLCanvasElement

data class ComponentDimensions(
    val bottomX: Double,
    val bottomY: Double,
    val width: Double,
    val height: Double,
    val radius: Double,
    val length: Double,
    val lineWidth: Double,
    val isHorizontal: Boolean = width > height
) {

    companion object {
        fun calculate(
            canvas: HTMLCanvasElement,
            xExpr: (CanvasDimensions) -> Double,
            yExpr: (CanvasDimensions) -> Double,
            widthExpr: (CanvasDimensions) -> Double,
            heightExpr: (CanvasDimensions) -> Double
        ) =
            canvas.dimensions().let { dim ->
                val width = widthExpr(dim)
                val height = heightExpr(dim)
                ComponentDimensions(
                    bottomX = xExpr(dim),
                    bottomY = yExpr(dim),
                    width = width,
                    height = height,
                    radius = if (width > height) height * 0.5 else width * 0.5,
                    length = if (width > height) width else height,
                    lineWidth = dim.vmin * 0.4
                )
            }
    }
}
