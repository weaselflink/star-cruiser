import de.bissell.starcruiser.Vector2
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.ROUND
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
    lineWidth = 2.0
    lineJoin = CanvasLineJoin.ROUND
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
        closePath()
    } else {
        val radius = width / 2.0
        moveTo(x, y - radius)
        lineTo(x, y - height + radius)
        arc(x + radius, y - height + radius, radius, PI, 0.0)
        lineTo(x + radius * 2, y - radius)
        arc(x + radius, y - radius, radius, 0.0, PI)
        closePath()
    }
}
