package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.MapSelectionMessage
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.formatThousands
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.pad
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.RIGHT

class MapSelectionDetails(
    private val canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double = { width - 42.vmin },
    private val yExpr: CanvasDimensions.() -> Double = { height - 2.vmin },
    private val widthExpr: CanvasDimensions.() -> Double = { 40.vmin },
    private val heightExpr: CanvasDimensions.() -> Double = { 62.vmin },
    private val onScan: () -> Unit,
    private val onDelete: () -> Unit
) : PointerEventHandlerParent() {

    private val ctx: CanvasRenderingContext2D = canvas.context2D
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { innerX },
        yExpr = { dim.topY + 32.vmin },
        widthExpr = { dim.width - 8.vmin },
        heightExpr = { 4.vmin }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { innerX },
        yExpr = { dim.topY + 38.vmin },
        widthExpr = { dim.width - 8.vmin },
        heightExpr = { 4.vmin }
    )
    private val actionButton = CanvasButton(
        canvas = canvas,
        xExpr = { dim.leftX + dim.width * 0.5 - 12.vmin },
        yExpr = { dim.bottomY - 5.vmin },
        widthExpr = { 24.vmin },
        onClick = { actionButtonClicked() }
    )
    private val spinner = CanvasSpinner(
        canvas = canvas,
        xExpr = { dim.leftX + 2.vmin },
        yExpr = { dim.topY + 9.vmin },
        widthExpr = { widthExpr() - 4.vmin },
        heightExpr = { 6.vmin },
        decreaseAction = { page = page.previous },
        decreaseEnabled = { page != page.previous },
        increaseAction = { page = page.next },
        increaseEnabled = { page != page.next },
        initialText = "Overview"
    )

    private var dim = calculateComponentDimensions()
    private var mapSelection: MapSelectionMessage? = null
    private val visible
        get() = mapSelection != null
    private val innerX
        get() = dim.leftX + dim.canvas.vmin * 4
    private var page = DetailsPage.Overview

    init {
        addChildren(actionButton, spinner)
    }

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return with(pointerEvent.point) {
            visible && x > dim.leftX && y > dim.topY &&
                x < dim.rightX && y < dim.bottomY
        }
    }

    fun draw(mapSelection: MapSelectionMessage?) {
        this.mapSelection = mapSelection?.also {
            ctx.draw(it)
        }
    }

    private fun actionButtonClicked() {
        mapSelection?.apply {
            when {
                canScan -> onScan()
                canDelete -> onDelete()
            }
        }
    }

    private fun CanvasRenderingContext2D.draw(mapSelection: MapSelectionMessage) {
        dim = calculateComponentDimensions()

        drawBase()
        drawDesignation(mapSelection.label)

        val hasDetails = mapSelection.systemsDamage != null
        if (page == DetailsPage.Systems && hasDetails) {
            drawDetails(mapSelection)
        } else {
            drawBasic(mapSelection)
        }
        if (hasDetails) {
            spinner.text = page.name
            spinner.draw()
        }
        when {
            mapSelection.canScan -> {
                actionButton.text = "Scan"
                actionButton.draw()
            }
            mapSelection.canDelete -> {
                actionButton.text = "Delete"
                actionButton.draw()
            }
        }
    }

    private fun CanvasRenderingContext2D.drawDetails(mapSelection: MapSelectionMessage) {
        mapSelection.systemsDamage?.entries?.forEachIndexed { index, system ->
            CanvasProgress(
                canvas = canvas,
                xExpr = { innerX },
                yExpr = { dim.topY + 24.vmin + 5.vmin * index },
                widthExpr = { dim.width - 8.vmin },
                heightExpr = { 4.vmin },
            ).apply {
                progress = system.value
                leftText = system.key.label
                rightText = "${system.value.toPercent()}%"
            }.draw()
        }
    }

    private fun CanvasRenderingContext2D.drawBasic(mapSelection: MapSelectionMessage) {
        drawBearing(mapSelection.bearing)
        drawRange(mapSelection.range)

        mapSelection.hullRatio?.also {
            hullDisplay.draw(it)
        }
        mapSelection.shield?.also {
            shieldsDisplay.draw(it)
        }
        mapSelection.shieldModulation?.also {
            drawShieldModulation(it)
        }
        mapSelection.beamModulation?.also {
            drawBeamModulation(it)
        }
    }

    private fun CanvasRenderingContext2D.drawBase() {
        save()

        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawRect(dim)
        fill()

        lineWidth = dim.lineWidth
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
        fillText(designation, innerX, dim.topY + dim.canvas.vmin * 16)

        restore()
    }

    private fun CanvasRenderingContext2D.drawBearing(bearing: Int) {
        val text = bearing.pad(3)
        drawAttribute(dim.canvas.vmin * 22, "Bearing", text)
    }

    private fun CanvasRenderingContext2D.drawRange(range: Int) {
        val text = range.formatThousands()
        drawAttribute(dim.canvas.vmin * 26, "Range", text)
    }

    private fun CanvasRenderingContext2D.drawShieldModulation(modulation: Int) {
        val value = modulation * 2 + 78
        val text = "$value PHz"
        drawAttribute(dim.canvas.vmin * 42, "∿ Shields", text)
    }

    private fun CanvasRenderingContext2D.drawBeamModulation(modulation: Int) {
        val value = modulation * 2 + 78
        val text = "$value PHz"
        drawAttribute(dim.canvas.vmin * 46, "∿ Beams", text)
    }

    private fun CanvasRenderingContext2D.drawAttribute(
        yOffset: Double,
        name: String,
        value: String
    ) {
        val rightX = dim.rightX - dim.canvas.vmin * 4
        val y = dim.topY + yOffset

        save()

        font = UiStyle.font(dim.canvas.vmin * 3)
        fillStyle = UiStyle.buttonForegroundColor
        fillText(name, innerX, y)

        textAlign = CanvasTextAlign.RIGHT
        fillText(value, rightX, y)

        restore()
    }

    private fun calculateComponentDimensions() =
        ComponentDimensions.calculateRect(
            canvas = canvas,
            xExpr = xExpr,
            yExpr = yExpr,
            widthExpr = widthExpr,
            heightExpr = heightExpr
        )
}

private enum class DetailsPage {
    Overview,
    Systems;

    val previous
        get() = when (this) {
            Overview -> Overview
            Systems -> Overview
        }

    val next
        get() = when (this) {
            Overview -> Systems
            Systems -> Systems
        }
}
