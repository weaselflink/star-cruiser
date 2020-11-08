package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import org.w3c.dom.HTMLCanvasElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun verticalButtonGroup(
    canvas: HTMLCanvasElement,
    rightXExpr: CanvasDimensions.() -> Double,
    topYExpr: CanvasDimensions.() -> Double,
    buttonWidthExpr: CanvasDimensions.() -> Double,
    buttonHeightExpr: CanvasDimensions.() -> Double,
    paddingExpr: CanvasDimensions.() -> Double = { 2.vmin },
    gapExpr: CanvasDimensions.() -> Double = { 1.vmin },
    block: VerticalButtonGroup.() -> Unit
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    VerticalButtonGroup(
        canvas = canvas,
        rightXExpr = rightXExpr,
        topYExpr = topYExpr,
        buttonWidthExpr = buttonWidthExpr,
        buttonHeightExpr = buttonHeightExpr,
        paddingExpr = paddingExpr,
        gapExpr = gapExpr
    ).apply(block)
}

class VerticalButtonGroup(
    private val canvas: HTMLCanvasElement,
    private val rightXExpr: CanvasDimensions.() -> Double,
    private val topYExpr: CanvasDimensions.() -> Double,
    private val buttonWidthExpr: CanvasDimensions.() -> Double,
    private val buttonHeightExpr: CanvasDimensions.() -> Double,
    private val paddingExpr: CanvasDimensions.() -> Double,
    private val gapExpr: CanvasDimensions.() -> Double,
) {

    private var index = 0
    private var gaps = 0

    fun addButton(
        onClick: () -> Unit = {},
        activated: () -> Boolean = { false },
        enabled: () -> Boolean = { true },
        initialText: String? = null
    ): CanvasButton {
        val currentIndex = index
        val currentGaps = gaps
        index++
        return CanvasButton(
            canvas = canvas,
            xExpr = { rightXExpr() - (buttonWidthExpr() + paddingExpr()) },
            yExpr = {
                topYExpr() + paddingExpr() +
                    buttonHeightExpr() +
                    buttonHeightExpr() * currentIndex +
                    gapExpr() * currentIndex +
                    gapExpr() * currentGaps
            },
            widthExpr = buttonWidthExpr,
            heightExpr = buttonHeightExpr,
            onClick = onClick,
            activated = activated,
            enabled = enabled,
            initialText = initialText
        )
    }

    fun addGap() {
        gaps++
    }
}
