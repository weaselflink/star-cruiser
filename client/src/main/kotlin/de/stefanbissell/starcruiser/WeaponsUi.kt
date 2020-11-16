package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandLockTarget
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.HullDisplay
import de.stefanbissell.starcruiser.components.LaunchTubeUi
import de.stefanbissell.starcruiser.components.ModulationUi
import de.stefanbissell.starcruiser.components.ShieldsDisplay
import de.stefanbissell.starcruiser.components.ShortRangeScope
import de.stefanbissell.starcruiser.components.StationUi
import org.w3c.dom.CanvasRenderingContext2D

class WeaponsUi : StationUi(Station.Weapons) {

    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }
    private val lockTargetButton = CanvasButton(
        canvas = canvas,
        xExpr = { width * 0.5 - 45.vmin },
        yExpr = { height * 0.5 - 38.vmin },
        widthExpr = { 25.vmin },
        onClick = { toggleLockTarget() },
        activated = { selectingTarget },
        initialText = "Lock on"
    )
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { 2.vmin },
        yExpr = { height - 34.vmin }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { 2.vmin },
        yExpr = { height - 26.vmin }
    )
    private val beamModulationUi = ModulationUi(
        canvas = canvas,
        xExpr = { 2.vmin },
        yExpr = { height - 45.vmin },
        decreaseCommand = Command.CommandDecreaseBeamModulation,
        increaseCommand = Command.CommandIncreaseBeamModulation
    )
    private val shieldModulationUi = ModulationUi(
        canvas = canvas,
        xExpr = { 2.vmin },
        yExpr = { height - 14.vmin },
        decreaseCommand = Command.CommandDecreaseShieldModulation,
        increaseCommand = Command.CommandIncreaseShieldModulation
    )
    private val shieldsButton = CanvasButton(
        canvas = canvas,
        xExpr = { 15.vmin },
        yExpr = { height - 2.vmin },
        widthExpr = { 20.vmin },
        onClick = { toggleShields() }
    )
    private val launchTubeUi = LaunchTubeUi(
        canvas = canvas,
        xExpr = { width - 48.vmin },
        yExpr = { height - 2.vmin }
    )

    private var selectingTarget = false

    init {
        addChildren(
            lockTargetButton,
            shieldsButton,
            beamModulationUi,
            shieldModulationUi,
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
        beamModulationUi.draw(snapshot.shortRangeScope.beams.modulation)
        shieldModulationUi.draw(snapshot.shield.modulation)
        launchTubeUi.draw(snapshot.tubes)
    }

    private fun contactSelected(targetId: ObjectId) {
        if (selectingTarget) {
            toggleLockTarget()
            ClientSocket.send(CommandLockTarget(targetId))
        }
    }

    private fun toggleShields() {
        ClientSocket.send(Command.CommandToggleShieldsUp)
    }

    private fun toggleLockTarget() {
        selectingTarget = !selectingTarget
    }
}
