package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.components.CanvasSlider
import org.w3c.dom.CanvasRenderingContext2D

class EngineeringUi : CanvasUi(Station.Engineering, "engineering-ui") {

    private val sliders = PoweredSystem.values().mapIndexed { index, system ->
        CanvasSlider(
            canvas = canvas,
            xExpr = { it.vmin * 3 },
            yExpr = { it.height - it.vmin * 3 - it.vmin * index * 12 },
            widthExpr = { it.vmin * 60 },
            heightExpr = { it.vmin * 10 },
            onChange = {},
            lines = listOf(0.5),
            leftText = system.name
        )
    }

    fun draw(snapshot: SnapshotMessage.Engineering) {
        val ship = snapshot.ship

        ctx.draw(snapshot, ship)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Engineering, ship: ShipMessage) {
        transformReset()
        clear("#222")

        sliders.forEach { it.draw(0.5) }
    }
}
