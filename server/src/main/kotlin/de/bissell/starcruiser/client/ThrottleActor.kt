package de.bissell.starcruiser.client

import de.bissell.starcruiser.client.ThrottleMessage.AcknowledgeInflightMessage
import de.bissell.starcruiser.client.ThrottleMessage.AddInflightMessage
import de.bissell.starcruiser.client.ThrottleMessage.GetInflightMessageCount
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor

sealed class ThrottleMessage {

    class AddInflightMessage(val response: CompletableDeferred<Long>) : ThrottleMessage()
    class GetInflightMessageCount(val response: CompletableDeferred<Int>) : ThrottleMessage()
    class AcknowledgeInflightMessage(val counter: Long) : ThrottleMessage()
}

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
