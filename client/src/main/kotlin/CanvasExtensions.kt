import de.bissell.starcruiser.Vector2
import org.w3c.dom.*
import kotlin.math.PI

fun CanvasRenderingContext2D.clear(color: String) {
    fillStyle = color
    fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
}

fun CanvasRenderingContext2D.translateToCenter() {
    translate(canvas.width / 2.0, canvas.height / 2.0)
}

fun CanvasRenderingContext2D.translate(vector: Vector2) =
    translate(vector.x, vector.y)

fun CanvasRenderingContext2D.circle(x: Double, y: Double, radius: Double) =
    ellipse(x, y, radius, radius, 0.0, 0.0, PI * 2)

val Int.px
    get() = "${this}px"

fun CanvasRenderingContext2D.drawShipSymbol(rot: Double, baseUnit: Double) {
    save()
    rotate(-rot)
    beginPath()
    moveTo(-baseUnit, -baseUnit)
    lineTo(baseUnit * 2, 0.0)
    lineTo(-baseUnit, baseUnit)
    lineTo(-baseUnit / 2, 0.0)
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

fun CanvasRenderingContext2D.historyStyle(dim: Double) {
    fillStyle = "#222"
    lineWidth = dim * 0.004
}

fun CanvasRenderingContext2D.shipStyle(dim: Double) {
    lineWidth = dim * 0.008 * 0.4
    lineJoin = CanvasLineJoin.ROUND
    strokeStyle = "#1e90ff"
    fillStyle = "#1e90ff"
    val textSize = (dim * 0.02).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
}

fun CanvasRenderingContext2D.contactStyle(dim: Double) {
    lineWidth = dim * 0.008 * 0.4
    lineJoin = CanvasLineJoin.ROUND
    strokeStyle = "#555"
    fillStyle = "#555"
    val textSize = (dim * 0.02).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
}

fun CanvasRenderingContext2D.wayPointStyle(dim: Double) {
    strokeStyle = "#4682B4"
    fillStyle = "#4682B4"
    lineWidth = dim * 0.004
    val textSize = (dim * 0.02).toInt()
    font = "bold ${textSize.px} sans-serif"
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
}
