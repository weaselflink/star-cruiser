package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.input.PointerEvent
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

    fun isInside(pointerEvent: PointerEvent): Boolean {
        val point = pointerEvent.point

        return point.x > bottomX && point.x < bottomX + width &&
                point.y > bottomY - height && point.y < bottomY
    }

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
                    lineWidth = dim.vmin * UiStyle.buttonLineWidth
                )
            }
    }
}
