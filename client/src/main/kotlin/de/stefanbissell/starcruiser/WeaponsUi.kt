package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandLockTarget
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.HullDisplay
import de.stefanbissell.starcruiser.components.ShieldsDisplay
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import de.stefanbissell.starcruiser.components.UiStyle
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline

class WeaponsUi : StationUi(Station.Weapons) {

    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }
    private val lockTargetButton = CanvasButton(
        canvas = canvas,
        xExpr = { width * 0.5 - vmin * 45 },
        yExpr = { height * 0.5 - vmin * 38 },
        widthExpr = { vmin * 25 },
        heightExpr = { vmin * 10 },
        onClick = { toggleLockTarget() },
        activated = { selectingTarget },
        initialText = "Lock on"
    )
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { vmin * 2 },
        yExpr = { height - vmin * 34 }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { vmin * 2 },
        yExpr = { height - vmin * 26 }
    )
    private val modulationDownButton = CanvasButton(
        canvas = canvas,
        xExpr = { vmin * 2 },
        yExpr = { height - vmin * 14 },
        widthExpr = { vmin * 12 },
        heightExpr = { vmin * 10 },
        onClick = { clientSocket.send(Command.CommandDecreaseShieldModulation) },
        initialText = "◄"
    )
    private val modulationUpButton = CanvasButton(
        canvas = canvas,
        xExpr = { vmin * 36 },
        yExpr = { height - vmin * 14 },
        widthExpr = { vmin * 12 },
        heightExpr = { vmin * 10 },
        onClick = { clientSocket.send(Command.CommandIncreaseShieldModulation) },
        initialText = "►"
    )
    private val shieldsButton = CanvasButton(
        canvas = canvas,
        xExpr = { vmin * 15 },
        yExpr = { height - vmin * 2 },
        widthExpr = { vmin * 20 },
        heightExpr = { vmin * 10 },
        onClick = { toggleShields() }
    )

    private var selectingTarget = false

    init {
        addChildren(
            lockTargetButton,
            shieldsButton,
            modulationDownButton,
            modulationUpButton,
            shortRangeScope
        )
    }

    fun draw(snapshot: SnapshotMessage.Weapons) {
        ctx.draw(snapshot)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Weapons) {
        transformReset()
        clearBackground()

        shortRangeScope.draw(snapshot)
        lockTargetButton.draw()
        hullDisplay.draw(snapshot.hull / snapshot.hullMax)
        shieldsDisplay.draw(snapshot.shield)
        shieldsButton.text = if (snapshot.shield.up) "Down" else "Up"
        shieldsButton.draw()

        drawModulation(snapshot)
        modulationDownButton.draw()
        modulationUpButton.draw()
    }

    private fun CanvasRenderingContext2D.drawModulation(snapshot: SnapshotMessage.Weapons) {
        val dim = canvas.dimensions()

        save()

        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawPill(
            x = dim.vmin * 2,
            y = dim.height - dim.vmin * 14,
            width = dim.vmin * 46,
            height = dim.vmin * 10
        )
        fill()

        fillStyle = UiStyle.buttonForegroundColor
        textAlign = CanvasTextAlign.CENTER
        textBaseline = CanvasTextBaseline.ALPHABETIC
        val textSize = dim.height * 0.05
        font = UiStyle.font(textSize)
        translate(dim.vmin * 25, dim.height - dim.vmin * 17.5)
        val text = "${(snapshot.shield.modulation * 2 + 78)} PHz"
        fillText(text, 0.0, 0.0)

        restore()
    }

    private fun contactSelected(targetId: ObjectId) {
        if (selectingTarget) {
            toggleLockTarget()
            clientSocket.send(CommandLockTarget(targetId))
        }
    }

    private fun toggleShields() {
        clientSocket.send(Command.CommandToggleShieldsUp)
    }

    private fun toggleLockTarget() {
        selectingTarget = !selectingTarget
    }
}
