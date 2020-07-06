import de.bissell.starcruiser.Vector2
import kotlin.browser.window
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND
import org.w3c.dom.TOP

val HTMLCanvasElement.context2D
    get() = getContext(contextId = "2d")!! as CanvasRenderingContext2D

fun CanvasRenderingContext2D.clear(color: String) {
    fillStyle = color
    fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
}

fun CanvasRenderingContext2D.translateToCenter() {
    translate(canvas.width / 2.0, canvas.height / 2.0)
}

fun CanvasRenderingContext2D.translate(vector: Vector2) =
    translate(vector.x, vector.y)

fun CanvasRenderingContext2D.circle(
    x: Double,
    y: Double,
    radius: Double,
    startAngle: Double = 0.0,
    endAngle: Double = PI * 2,
    anticlockwise: Boolean = false
) = ellipse(x, y, radius, radius, 0.0, startAngle, endAngle, anticlockwise)

val Int.px
    get() = "${this}px"

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

fun CanvasRenderingContext2D.drawPill(x: Double, y: Double, width: Double, height: Double) {
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

fun CanvasRenderingContext2D.historyStyle(dim: CanvasDimensions) {
    fillStyle = "#555"
    lineWidth = dim.vmin * 0.4
}

fun CanvasRenderingContext2D.shipStyle(dim: CanvasDimensions) {
    lineWidth = dim.vmin * 0.3
    lineJoin = CanvasLineJoin.ROUND
    strokeStyle = "#ffffff"
}

fun CanvasRenderingContext2D.beamStyle(dim: CanvasDimensions) {
    strokeStyle = "#dc143c"
    lineWidth = dim.vmin * 0.3
}

fun CanvasRenderingContext2D.unknownContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#555"
    fillStyle = "#555"
    contactStyle(dim)
}

fun CanvasRenderingContext2D.environmentContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#997300"
    fillStyle = "#997300"
    contactStyle(dim)
}

fun CanvasRenderingContext2D.friendlyContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#1e90ff"
    fillStyle = "#1e90ff"
    contactStyle(dim)
}

private fun CanvasRenderingContext2D.contactStyle(dim: CanvasDimensions) {
    lineWidth = dim.vmin * 0.3
    lineJoin = CanvasLineJoin.ROUND
    val textSize = (dim.vmin * 2).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
}

fun CanvasRenderingContext2D.wayPointStyle(dim: CanvasDimensions) {
    strokeStyle = "#4682b4"
    fillStyle = "#4682b4"
    lineWidth = dim.vmin * 0.4
    val textSize = (dim.vmin * 2).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.scanProgressStyle(dim: CanvasDimensions) {
    strokeStyle = "#ff6347"
    fillStyle = "#ff6347"
    lineWidth = dim.vmin * 0.5
    val textSize = (dim.vmin * 4).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
    textBaseline = CanvasTextBaseline.TOP
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.lockMarkerStyle(dim: CanvasDimensions) {
    strokeStyle = "#dc143c"
    fillStyle = "#dc143c"
    lineWidth = dim.vmin * 0.3
    val textSize = (dim.vmin * 3).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.selectionMarkerStyle(dim: CanvasDimensions) {
    strokeStyle = "#666"
    fillStyle = "#666"
    lineWidth = dim.vmin * 0.3
    val textSize = (dim.vmin * 3).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
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
}

fun CanvasRenderingContext2D.transformReset() = setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
