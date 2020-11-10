package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.ComponentDimensions
import de.stefanbissell.starcruiser.components.UiStyle
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.Document
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

val Document.canvas2d
    get() = querySelector(".canvas2d") as HTMLCanvasElement

val Document.canvas3d
    get() = querySelector(".canvas3d") as HTMLCanvasElement

val HTMLCanvasElement.context2D
    get() = getContext(contextId = "2d")!! as CanvasRenderingContext2D

fun CanvasRenderingContext2D.clear() {
    clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
}

fun CanvasRenderingContext2D.clearBackground() {
    clear(UiStyle.backgroundColor)
}

fun CanvasRenderingContext2D.clear(color: String) {
    fillStyle = color
    fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
}

fun CanvasRenderingContext2D.translateToCenter() {
    translate(canvas.width / 2.0, canvas.height / 2.0)
}

fun CanvasRenderingContext2D.translate(vector: Vector2) =
    translate(vector.x, vector.y)

fun CanvasRenderingContext2D.moveTo(vector: Vector2) =
    moveTo(vector.x, vector.y)

fun CanvasRenderingContext2D.lineTo(vector: Vector2) =
    lineTo(vector.x, vector.y)

fun CanvasRenderingContext2D.circle(
    center: Vector2,
    radius: Double,
    startAngle: Double = 0.0,
    endAngle: Double = PI * 2,
    anticlockwise: Boolean = false
) = ellipse(center.x, center.y, radius, radius, 0.0, startAngle, endAngle, anticlockwise)

fun CanvasRenderingContext2D.circle(
    x: Double,
    y: Double,
    radius: Double,
    startAngle: Double = 0.0,
    endAngle: Double = PI * 2,
    anticlockwise: Boolean = false
) = ellipse(x, y, radius, radius, 0.0, startAngle, endAngle, anticlockwise)

fun CanvasRenderingContext2D.drawShipSymbol(rot: Double, baseUnit: Double) {
    save()
    rotate(-rot)
    beginPath()
    moveTo(baseUnit * -1.4, baseUnit * -1.0)
    lineTo(baseUnit * 1.6, baseUnit * 0.0)
    lineTo(baseUnit * -1.4, baseUnit * 1.0)
    lineTo(baseUnit * -0.9, baseUnit * 0.0)
    closePath()
    stroke()
    restore()
}

fun CanvasRenderingContext2D.drawAsteroidSymbol(rot: Double, baseUnit: Double) {
    save()
    rotate(-rot)
    beginPath()
    moveTo(baseUnit * 0.0, baseUnit * -1.4)
    lineTo(baseUnit * 1.0, baseUnit * -1.2)
    lineTo(baseUnit * 1.4, baseUnit * -0.4)
    lineTo(baseUnit * 1.2, baseUnit * 0.2)
    lineTo(baseUnit * 1.2, baseUnit * 0.2)
    lineTo(baseUnit * 1.4, baseUnit * 0.9)
    lineTo(baseUnit * 0.0, baseUnit * 1.5)
    lineTo(baseUnit * -0.5, baseUnit * 1.0)
    lineTo(baseUnit * -1.1, baseUnit * 1.0)
    lineTo(baseUnit * -1.4, baseUnit * -0.2)
    lineTo(baseUnit * -1.0, baseUnit * -0.7)
    lineTo(baseUnit * -0.8, baseUnit * -1.2)
    closePath()
    stroke()
    restore()
}

fun CanvasRenderingContext2D.drawLockMarker(baseUnit: Double) {
    save()
    beginPath()
    moveTo(0.0, -baseUnit)
    lineTo(-baseUnit, 0.0)
    lineTo(0.0, baseUnit)
    lineTo(baseUnit, 0.0)
    closePath()
    stroke()
    restore()
}

fun CanvasRenderingContext2D.drawPill(dim: ComponentDimensions) {
    drawPill(dim.leftX, dim.bottomY, dim.width, dim.height)
}

fun CanvasRenderingContext2D.drawPill(
    x: Double,
    y: Double,
    width: Double,
    height: Double
) {
    if (width > height) {
        val radius = height / 2.0
        moveTo(x + radius, y - radius * 2)
        lineTo(x + width - radius, y - radius * 2)
        arc(x + width - radius, y - radius, radius, -(PI / 2.0), PI / 2.0)
        lineTo(x + radius, y)
        arc(x + radius, y - radius, radius, PI / 2.0, -(PI / 2.0))
    } else {
        val radius = width / 2.0
        moveTo(x, y - radius)
        lineTo(x, y - height + radius)
        arc(x + radius, y - height + radius, radius, PI, 0.0)
        lineTo(x + radius * 2, y - radius)
        arc(x + radius, y - radius, radius, 0.0, PI)
    }
    closePath()
}

fun CanvasRenderingContext2D.drawRect(dim: ComponentDimensions) {
    drawRect(dim.leftX, dim.bottomY, dim.width, dim.height, dim.radius)
}

fun CanvasRenderingContext2D.drawRect(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    radius: Double
) {

    moveTo(x + width - radius, y - height)
    arc(x + width - radius, y - height + radius, radius, -(PI / 2.0), 0.0)
    lineTo(x + width, y - radius)
    arc(x + width - radius, y - radius, radius, 0.0, PI / 2.0)
    lineTo(x + radius, y)
    arc(x + radius, y - radius, radius, PI / 2.0, PI)
    lineTo(x, y - height + radius)
    arc(x + radius, y - height + radius, radius, PI, -(PI / 2.0))
    closePath()
}

fun HTMLCanvasElement.updateSize(square: Boolean = false) {
    val windowWidth = window.innerWidth
    val windowHeight = window.innerHeight
    val dim = min(windowWidth, windowHeight)
    val newWidth = if (square) dim else windowWidth
    val newHeight = if (square) dim else windowHeight

    if (width != newWidth || height != newHeight) {
        width = newWidth
        height = newHeight
    }

    style.left = ((windowWidth - newWidth) / 2).px
    style.top = ((windowHeight - newHeight) / 2).px
    style.width = newWidth.px
    style.height = newHeight.px
}

fun HTMLCanvasElement.dimensions() = CanvasDimensions(width, height)

data class CanvasDimensions(
    val width: Double,
    val height: Double,
    val min: Double = min(width, height),
    val max: Double = max(width, height),
    val vw: Double = width / 100.0,
    val vh: Double = width / 100.0,
    val vmin: Double = min / 100.0,
    val vmax: Double = min / 100.0,
    val isLandscape: Boolean = height > width
) {
    constructor(
        width: Number,
        height: Number
    ) : this(width.toDouble(), height.toDouble())

    val halfWidth = width * 0.5

    val Int.vmin
        get() = this@CanvasDimensions.vmin * this

    val Double.vmin
        get() = this@CanvasDimensions.vmin * this
}

fun CanvasRenderingContext2D.transformReset() = setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

operator fun (CanvasDimensions.() -> Double).plus(other: CanvasDimensions.() -> Double): CanvasDimensions.() -> Double =
    { this@plus() + other() }

operator fun (CanvasDimensions.() -> Double).minus(other: CanvasDimensions.() -> Double): CanvasDimensions.() -> Double =
    { this@minus() - other() }
