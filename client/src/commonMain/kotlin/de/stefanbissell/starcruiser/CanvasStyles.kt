package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.UiStyle
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.ROUND
import org.w3c.dom.TOP

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

fun CanvasRenderingContext2D.tubeStyle(dim: CanvasDimensions) {
    strokeStyle = "#dc143c50"
    lineWidth = dim.vmin * 0.3
    lineCap = CanvasLineCap.ROUND
    setLineDash(arrayOf(dim.vmin * 2, dim.vmin * 2))
}

fun CanvasRenderingContext2D.unknownContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#555"
    fillStyle = "#555"
    contactStyle(dim)
}

fun CanvasRenderingContext2D.environmentContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#997300"
    fillStyle = "#99730020"
    contactStyle(dim)
}

fun CanvasRenderingContext2D.friendlyContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#1e90ff"
    fillStyle = "#1e90ff"
    contactStyle(dim)
}

fun CanvasRenderingContext2D.enemyContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#ff6347"
    fillStyle = "#ff6347"
    contactStyle(dim)
}

fun CanvasRenderingContext2D.neutralContactStyle(dim: CanvasDimensions) {
    strokeStyle = "#ffd700"
    fillStyle = "#ffd700"
    contactStyle(dim)
}

private fun CanvasRenderingContext2D.contactStyle(dim: CanvasDimensions) {
    lineWidth = dim.vmin * 0.3
    lineJoin = CanvasLineJoin.ROUND
    val textSize = (dim.vmin * 2).toInt()
    font = UiStyle.boldFont(textSize)
    textAlign = CanvasTextAlign.CENTER
}

fun CanvasRenderingContext2D.wayPointStyle(dim: CanvasDimensions) {
    strokeStyle = "#4682b4"
    fillStyle = "#4682b4"
    lineWidth = dim.vmin * 0.4
    val textSize = (dim.vmin * 2).toInt()
    font = UiStyle.boldFont(textSize)
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.scanProgressStyle(dim: CanvasDimensions) {
    strokeStyle = "#ff6347"
    fillStyle = "#ff6347"
    lineWidth = dim.vmin * 0.5
    val textSize = (dim.vmin * 4).toInt()
    font = UiStyle.boldFont(textSize)
    textAlign = CanvasTextAlign.CENTER
    textBaseline = CanvasTextBaseline.TOP
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.lockMarkerStyle(dim: CanvasDimensions) {
    strokeStyle = "#dc143c"
    fillStyle = "#dc143c"
    lineWidth = dim.vmin * 0.3
    val textSize = (dim.vmin * 3).toInt()
    font = UiStyle.boldFont(textSize)
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.selectionMarkerStyle(dim: CanvasDimensions) {
    strokeStyle = "#666"
    fillStyle = "#666"
    lineWidth = dim.vmin * 0.3
    val textSize = (dim.vmin * 3).toInt()
    font = UiStyle.boldFont(textSize)
    textAlign = CanvasTextAlign.CENTER
    lineJoin = CanvasLineJoin.ROUND
}

fun CanvasRenderingContext2D.sensorRangeStyle(dim: CanvasDimensions) {
    strokeStyle = "#801a00"
    lineWidth = dim.vmin * 0.5
    setLineDash(arrayOf(dim.vmin * 2, dim.vmin * 2))
}
