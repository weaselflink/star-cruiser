package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.ships.ShipContactList

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

    private fun ShipContactList.selectScanTarget(): ShipContactList.ShipContact? {
        val (friendlies, rest) = allInSensorRange()
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
