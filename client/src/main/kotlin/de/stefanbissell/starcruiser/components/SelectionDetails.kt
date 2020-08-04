package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.pad
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

class SelectionDetails(
    private val canvas: HTMLCanvasElement,
    private val xExpr: (CanvasDimensions) -> Double = { it.width - it.vmin * 42 },
    private val yExpr: (CanvasDimensions) -> Double = { it.height - it.vmin * 2 },
    private val widthExpr: (CanvasDimensions) -> Double = { it.vmin * 40 },
    private val heightExpr: (CanvasDimensions) -> Double = { it.vmin * 52 },
    private val onScan: () -> Unit,
    private val onDelete: () -> Unit
) : PointerEventHandlerParent() {

    private val ctx: CanvasRenderingContext2D = canvas.context2D
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { innerX },
        yExpr = { dim.bottomY - dim.height + it.vmin * 22 },
        widthExpr = { dim.width - it.vmin * 8 },
        heightExpr = { it.vmin * 4 }
    )
    private val actionButton = CanvasButton(
        canvas = canvas,
        xExpr = { dim.bottomX + dim.width * 0.5 - it.vmin * 12 },
        yExpr = { dim.bottomY - it.vmin * 5 },
        widthExpr = { it.vmin * 24 },
        heightExpr = { it.vmin * 10 },
        onClick = { actionButtonClicked() }
    )

    private var dim = calculateComponentDimensions()
    private var selection: Selection? = null
    private val visible
        get() = selection != null
    private val innerX
        get() = dim.bottomX + dim.canvas.vmin * 4

    init {
        addChildren(actionButton)
    }

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return with(pointerEvent.point) {
            visible && x > dim.bottomX && y > dim.bottomY - dim.height &&
                x < dim.bottomX + dim.width && y < dim.bottomY
        }
    }

    fun draw(selection: Selection?) {
        this.selection = selection?.also {
            ctx.draw(it)
        }
    }

    private fun actionButtonClicked() {
        selection?.apply {
            when {
                canScan -> onScan()
                canDelete -> onDelete()
            }
        }
    }

    private fun CanvasRenderingContext2D.draw(selection: Selection) {
        dim = calculateComponentDimensions()

        drawBase()
        drawDesignation(selection.label)
        drawBearing(selection.bearing)
        drawRange(selection.range)

        selection.hullRatio?.also {
            hullDisplay.draw(it)
        }
        when {
            selection.canScan -> {
                actionButton.text = "Scan"
                actionButton.draw()
            }
            selection.canDelete -> {
                actionButton.text = "Delete"
                actionButton.draw()
            }
        }
    }

    private fun CanvasRenderingContext2D.drawBase() {
        save()

        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawRect(dim)
        fill()

        strokeStyle = UiStyle.buttonForegroundColor
        beginPath()
        drawRect(dim)
        stroke()

        restore()
    }

    private fun CanvasRenderingContext2D.drawDesignation(designation: String) {
        save()

        font = UiStyle.boldFont(dim.canvas.vmin * 3)
        fillStyle = UiStyle.buttonForegroundColor
        fillText(designation, innerX, dim.bottomY - dim.height + dim.canvas.vmin * 6)

        restore()
    }

    private fun CanvasRenderingContext2D.drawBearing(bearing: Double) {
        save()

        font = UiStyle.font(dim.canvas.vmin * 3)
        fillStyle = UiStyle.buttonForegroundColor
        fillText(
            "Bearing",
            innerX,
            dim.bottomY - dim.height + dim.canvas.vmin * 12
        )
        val text = bearing.roundToInt().pad(3)
        fillText(
            text,
            dim.bottomX + dim.width - dim.canvas.vmin * 10,
            dim.bottomY - dim.height + dim.canvas.vmin * 12
        )

        restore()
    }

    private fun CanvasRenderingContext2D.drawRange(range: Double) {
        save()

        font = UiStyle.font(dim.canvas.vmin * 3)
        fillStyle = UiStyle.buttonForegroundColor
        fillText(
            "Range",
            innerX,
            dim.bottomY - dim.height + dim.canvas.vmin * 16
        )
        val text = range.roundToInt().toString()
        fillText(
            text,
            dim.bottomX + dim.width - dim.canvas.vmin * 10,
            dim.bottomY - dim.height + dim.canvas.vmin * 16
        )

        restore()
    }

    private fun calculateComponentDimensions() =
        ComponentDimensions.calculateRect(canvas, xExpr, yExpr, widthExpr, heightExpr)
}

