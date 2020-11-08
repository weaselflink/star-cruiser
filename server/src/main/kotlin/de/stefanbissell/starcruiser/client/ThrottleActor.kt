package de.stefanbissell.starcruiser.client

import de.stefanbissell.starcruiser.client.ThrottleMessage.AcknowledgeInflightMessage
import de.stefanbissell.starcruiser.client.ThrottleMessage.AddInflightMessage
import de.stefanbissell.starcruiser.client.ThrottleMessage.GetInflightMessageCount
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

sealed class ThrottleMessage {

    class AddInflightMessage(val response: CompletableDeferred<Long>) : ThrottleMessage()
    class GetInflightMessageCount(val response: CompletableDeferred<Int>) : ThrottleMessage()
    class AcknowledgeInflightMessage(val counter: Long) : ThrottleMessage()
}

fun CoroutineScope.createThrottleActor(): SendChannel<ThrottleMessage> =
    Channel<ThrottleMessage>().also { channel ->
        launch {
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
    }
