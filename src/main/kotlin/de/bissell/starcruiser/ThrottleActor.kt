package de.bissell.starcruiser

import de.bissell.starcruiser.ThrottleMessage.AcknowledgeInflightMessage
import de.bissell.starcruiser.ThrottleMessage.AddInflightMessage
import de.bissell.starcruiser.ThrottleMessage.GetInflightMessageCount
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor

sealed class ThrottleMessage {

    class AddInflightMessage(val response: CompletableDeferred<Long>) : ThrottleMessage()
    class GetInflightMessageCount(val response: CompletableDeferred<Int>) : ThrottleMessage()
    class AcknowledgeInflightMessage(val counter: Long) : ThrottleMessage()
}

@ObsoleteCoroutinesApi
fun CoroutineScope.createThrottleActor() = actor<ThrottleMessage> {
    var updateCounter: Long = 0
    val inflightUpdates: MutableList<Long> = mutableListOf()

    for (message in channel) {
        when (message) {
            is AddInflightMessage -> {
                updateCounter++
                inflightUpdates += updateCounter
                message.response.complete(updateCounter)
            }
            is GetInflightMessageCount -> message.response.complete(inflightUpdates.size)
            is AcknowledgeInflightMessage -> inflightUpdates -= message.counter
        }
    }
}
