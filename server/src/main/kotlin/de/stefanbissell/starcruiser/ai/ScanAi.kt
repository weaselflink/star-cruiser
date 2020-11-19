package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.onlyVessels

class ScanAi : ComponentAi(5.0) {

    override fun execute(aiState: AiState) {
        aiState.updateScan()
    }

    private fun AiState.updateScan() {
        if (ship.scanHandler == null) {
            contactList.selectScanTarget()?.also {
                ship.startScan(it.id)
            }
        }
    }

    private fun ContactList.selectScanTarget(): ContactList.Contact? {
        val (friendlies, rest) = allInSensorRange()
            .onlyVessels()
            .filter {
                it.scanLevel.canBeIncreased
            }
            .partition {
                it.contactType == ContactType.Friendly
            }
        return rest.minByOrNull {
            it.range
        } ?: friendlies.minByOrNull {
            it.range
        }
    }
}
