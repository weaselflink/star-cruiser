package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandLockTarget
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.HullDisplay
import de.stefanbissell.starcruiser.components.ShieldsDisplay
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import org.w3c.dom.CanvasRenderingContext2D

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
        initialText = "◄"
    )
    private val modulationUpButton = CanvasButton(
        canvas = canvas,
        xExpr = { vmin * 30 },
        yExpr = { height - vmin * 14 },
        widthExpr = { vmin * 12 },
        heightExpr = { vmin * 10 },
        initialText = "►"
    )
    private val shieldsButton = CanvasButton(
        canvas = canvas,
        xExpr = { vmin * 12 },
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
        modulationDownButton.draw()
        modulationUpButton.draw()
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
