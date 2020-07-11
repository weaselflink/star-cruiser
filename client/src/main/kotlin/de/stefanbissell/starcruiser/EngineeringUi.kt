package de.stefanbissell.starcruiser

import org.w3c.dom.CanvasRenderingContext2D

class EngineeringUi : CanvasUi(Station.Engineering, "engineering-ui") {

    fun draw(snapshot: SnapshotMessage.Engineering) {
        val ship = snapshot.ship

        ctx.draw(snapshot, ship)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Engineering, ship: ShipMessage) {
        transformReset()
        clear("#222")
    }
}
