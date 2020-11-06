package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.PoweredSystemType

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

    override fun execute(aiState: AiState) {
        with(aiState.ship.powerHandler) {
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
