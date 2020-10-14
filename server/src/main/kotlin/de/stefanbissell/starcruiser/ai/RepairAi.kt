package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class RepairAi : ComponentAi() {

    private val priorityList = listOf(
        PoweredSystemType.Shields,
        PoweredSystemType.Impulse,
        PoweredSystemType.Maneuver,
        PoweredSystemType.Weapons,
        PoweredSystemType.Sensors,
        PoweredSystemType.Jump,
        PoweredSystemType.Reactor
    )

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        with(ship.powerHandler) {
            if (!repairing) {
                priorityList.mapNotNull {
                    poweredSystems[it]
                }.firstOrNull {
                    it.canRepair()
                }?.also {
                    startRepair(it.type)
                }
            }
        }
    }
}
