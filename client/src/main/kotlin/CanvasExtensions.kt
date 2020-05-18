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
