package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandChangeJumpDistance
import de.stefanbissell.starcruiser.Command.CommandChangeRudder
import de.stefanbissell.starcruiser.Command.CommandChangeThrottle
import de.stefanbissell.starcruiser.Command.CommandStartJump
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.CanvasSlider
import de.stefanbissell.starcruiser.components.JumpDisplay
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HelmUi : StationUi(Station.Helm) {

    private val shortRangeScope = ShortRangeScope(canvas)
    private val throttleSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { vmin * 3 },
        yExpr = { height - vmin * 3 },
        widthExpr = { vmin * 10 },
        heightExpr = { vmin * 60 },
        onChange = {
            val throttle = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            ClientSocket.send(CommandChangeThrottle(throttle))
        },
        lines = listOf(0.5),
        leftText = "Impulse"
    )
    private val jumpSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { vmin * 15 },
        yExpr = { height - vmin * 3 },
        widthExpr = { vmin * 10 },
        heightExpr = { vmin * 60 },
        onChange = {
            ClientSocket.send(CommandChangeJumpDistance(it))
        },
        leftText = "Distance"
    )
    private val rudderSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { width - vmin * 63 },
        yExpr = { height - vmin * 3 },
        widthExpr = { vmin * 60 },
        heightExpr = { vmin * 10 },
        onChange = {
            val rudder = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            ClientSocket.send(CommandChangeRudder(rudder))
        },
        lines = listOf(0.5),
        leftText = "Rudder",
        reverseValue = true
    )
    private val jumpDisplay = JumpDisplay(
        canvas = canvas,
        xExpr = { vmin * 27 },
        yExpr = { height - if (width >= vmin * 125) vmin * 15 else vmin * 27 }
    )
    private val jumpButton = CanvasButton(
        canvas = canvas,
        xExpr = { vmin * 34 },
        yExpr = { height - if (width >= vmin * 125) vmin * 3 else vmin * 15 },
        widthExpr = { vmin * 20 },
        heightExpr = { vmin * 10 },
        onClick = { ClientSocket.send(CommandStartJump) },
        initialText = "Jump"
    )

    init {
        addChildren(
            throttleSlider,
            jumpSlider,
            rudderSlider,
            jumpButton,
            shortRangeScope
        )
    }

    fun draw(snapshot: SnapshotMessage.Helm) {
        ctx.draw(snapshot)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Helm) {
        transformReset()
        clearBackground()

        shortRangeScope.draw(snapshot)

        drawThrottle(snapshot)
        drawJump(snapshot)
        drawRudder(snapshot)
    }

    private fun drawThrottle(snapshot: SnapshotMessage.Helm) =
        throttleSlider.draw((snapshot.throttle + 100) / 200.0)

    private fun drawJump(snapshot: SnapshotMessage.Helm) {
        jumpSlider.draw(snapshot.jumpDrive.ratio)
        jumpDisplay.draw(snapshot.jumpDrive)
        jumpButton.draw()
    }

    private fun drawRudder(snapshot: SnapshotMessage.Helm) =
        rudderSlider.draw((snapshot.rudder + 100) / 200.0)
}
