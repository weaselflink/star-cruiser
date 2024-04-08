package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.minus
import de.stefanbissell.starcruiser.plus
import org.w3c.dom.HTMLCanvasElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun verticalButtonGroup(
    canvas: HTMLCanvasElement,
    leftXExpr: (CanvasDimensions.() -> Double)? = null,
    rightXExpr: (CanvasDimensions.() -> Double)? = null,
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

    val safeLeftXExpr: CanvasDimensions.() -> Double =
        when {
            leftXExpr != null -> leftXExpr + paddingExpr
            rightXExpr != null -> rightXExpr - (buttonWidthExpr + paddingExpr)
            else -> ({ 0.vmin })
        }

    VerticalButtonGroup(
        canvas = canvas,
        leftXExpr = safeLeftXExpr,
        topYExpr = topYExpr,
        buttonWidthExpr = buttonWidthExpr,
        buttonHeightExpr = buttonHeightExpr,
        paddingExpr = paddingExpr,
        gapExpr = gapExpr
    ).apply(block)
}

class VerticalButtonGroup(
    private val canvas: HTMLCanvasElement,
    private val leftXExpr: CanvasDimensions.() -> Double,
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
            xExpr = leftXExpr,
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
