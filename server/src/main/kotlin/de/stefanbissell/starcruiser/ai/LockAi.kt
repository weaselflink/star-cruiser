package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.ships.ShipContactList

class LockAi : ComponentAi(5.0) {

    override fun execute(aiState: AiState) {
        aiState.updateLock()
    }

    private fun AiState.updateLock() {
        val lockTarget = ship.lockHandler?.targetId?.let { contactList[it] }
        if (lockTarget != null && !lockTarget.nearScopeRange) {
            ship.abortLock()
        }
        if (ship.lockHandler == null) {
            contactList.selectLockTarget()?.also {
                ship.startLock(it.id)
            }
        }
    }

    private fun ShipContactList.selectLockTarget() =
        allNearScopeRange()
            .filter {
                it.contactType == ContactType.Enemy
            }
            .minByOrNull { it.range }
}
