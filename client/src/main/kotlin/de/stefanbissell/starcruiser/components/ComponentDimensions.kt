package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.input.PointerEvent
import org.w3c.dom.HTMLCanvasElement

data class ComponentDimensions(
    val canvas: CanvasDimensions,
    val leftX: Double,
    val bottomY: Double,
    val width: Double,
    val height: Double,
    val rightX: Double = leftX + width,
    val topY: Double = bottomY - height,
    val radius: Double,
    val length: Double,
    val lineWidth: Double,
    val isHorizontal: Boolean = width > height
) {

    fun isInside(pointerEvent: PointerEvent): Boolean {
        val point = pointerEvent.point

        return point.x > leftX && point.x < rightX &&
            point.y > topY && point.y < bottomY
    }

    companion object {
        fun calculatePill(
            canvas: HTMLCanvasElement,
            xExpr: CanvasDimensions.() -> Double,
            yExpr: CanvasDimensions.() -> Double,
            widthExpr: CanvasDimensions.() -> Double,
            heightExpr: CanvasDimensions.() -> Double
        ) =
            canvas.dimensions().let { dim ->
                val width = dim.widthExpr()
                val height = dim.heightExpr()
                ComponentDimensions(
                    canvas = dim,
                    leftX = dim.xExpr(),
                    bottomY = dim.yExpr(),
                    width = width,
                    height = height,
                    radius = if (width > height) height * 0.5 else width * 0.5,
                    length = if (width > height) width else height,
                    lineWidth = dim.vmin * UiStyle.buttonLineWidth
                )
            }

        fun calculateRect(
            canvas: HTMLCanvasElement,
            xExpr: CanvasDimensions.() -> Double,
            yExpr: CanvasDimensions.() -> Double,
            widthExpr: CanvasDimensions.() -> Double,
            heightExpr: CanvasDimensions.() -> Double
        ) =
            canvas.dimensions().let { dim ->
                ComponentDimensions(
                    canvas = dim,
                    leftX = dim.xExpr(),
                    bottomY = dim.yExpr(),
                    width = dim.widthExpr(),
                    height = dim.heightExpr(),
                    radius = dim.vmin * 5,
                    length = dim.widthExpr(),
                    lineWidth = dim.vmin * UiStyle.buttonLineWidth
                )
            }
    }
}
