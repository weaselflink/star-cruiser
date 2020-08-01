package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandLockTarget
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.HullDisplay
import de.stefanbissell.starcruiser.components.ShieldsDisplay
import de.stefanbissell.starcruiser.components.ShortRangeScope
import org.w3c.dom.CanvasRenderingContext2D

class WeaponsUi : CanvasUi(Station.Weapons, "weapons-ui") {

    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }
    private val lockTargetButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 45 },
        yExpr = { it.height * 0.5 - it.vmin * 38 },
        widthExpr = { it.vmin * 25 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleLockTarget() },
        activated = { selectingTarget },
        initialText = "Lock on"
    )
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 23 }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 15 }
    )
    private val shieldsButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.vmin * 13 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleShields() }
    )

    private var shieldsUp = false
    private var selectingTarget = false

    init {
        pointerEventDispatcher.addHandlers(
            shortRangeScope.rotateButton,
            lockTargetButton,
            shieldsButton
        )
    }

    fun draw(snapshot: SnapshotMessage.Weapons) {
        shieldsUp = snapshot.shield.up

        ctx.draw(snapshot)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Weapons) {
        transformReset()
        clearBackground()

        shortRangeScope.draw(snapshot)
        lockTargetButton.draw()
        hullDisplay.draw(snapshot)
        shieldsDisplay.draw(snapshot.shield)
        shieldsButton.text = if (snapshot.shield.up) "Down" else "Up"
        shieldsButton.draw()
    }

    private fun contactSelected(targetId: ObjectId) {
        if (selectingTarget) {
            toggleLockTarget()
            clientSocket.send(CommandLockTarget(targetId))
        }
    }

    private fun toggleShields() {
        clientSocket.send(Command.CommandSetShieldsUp(!shieldsUp))
    }

    private fun toggleLockTarget() {
        selectingTarget = !selectingTarget
    }
}
